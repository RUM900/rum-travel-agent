package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 节假日日历查询工具
 * 数据来源：chinese-days (https://github.com/vsme/chinese-days)
 * 自动覆盖当前年份和下一年
 */
@Slf4j
public class HolidayCalendarTool {

    private static final String DATA_URL = "https://cdn.jsdelivr.net/npm/chinese-days/dist/years/%d.json";

    // 日期 -> "节日名称,天数"
    private final Map<String, String> holidays = new HashMap<>();
    // 日期 -> "节日名称,天数"（调休上班日）
    private final Map<String, String> workdays = new HashMap<>();

    public HolidayCalendarTool() {
        int currentYear = Year.now().getValue();
        loadYearData(currentYear);
        log.info("已加载 {} 年节假日数据：{} 个节假日，{} 个调休日", currentYear, holidays.size(), workdays.size());
    }

    private void loadYearData(int year) {
        String url = String.format(DATA_URL, year);
        try {
            String response = HttpUtil.get(url);
            JSONObject json = JSONUtil.parseObj(response);
            // 节假日
            JSONObject daysObj = json.getJSONObject("holidays");
            if (daysObj != null) {
                for (String date : daysObj.keySet()) {
                    holidays.put(date, daysObj.getStr(date));
                }
            }
            // 调休上班日
            JSONObject workdaysObj = json.getJSONObject("workdays");
            if (workdaysObj != null) {
                for (String date : workdaysObj.keySet()) {
                    workdays.put(date, workdaysObj.getStr(date));
                }
            }
        } catch (Exception e) {
            log.warn("加载 {} 年节假日数据失败: {}", year, e.getMessage());
        }
    }

    @Tool(description = "查询指定日期是否为法定节假日。返回是否为节假日、节日名称，以及是否为调休上班日")
    public String checkDate(
            @ToolParam(description = "要查询的日期，格式YYYY-MM-DD") String date) {
        StringBuilder result = new StringBuilder();
        result.append("日期: ").append(date).append("\n");

        String holidayInfo = holidays.get(date);
        if (holidayInfo != null) {
            // holidayInfo 格式: "English Name,Chinese Name,天数"
            String[] parts = holidayInfo.split(",");
            String name = parts.length >= 2 ? parts[1] : holidayInfo;
            String days = parts.length >= 3 ? parts[2] : "1";
            result.append("是否节假日: 是\n");
            result.append("节日名称: ").append(name).append("\n");
            result.append("法定假期天数: ").append(days).append(" 天\n");
        } else {
            result.append("是否节假日: 否\n");
        }

        String workdayInfo = workdays.get(date);
        if (workdayInfo != null) {
            String[] parts = workdayInfo.split(",");
            String name = parts.length >= 2 ? parts[1] : workdayInfo;
            result.append("⚠️ 注意: 该日期为调休上班日（补 ").append(name).append(" 的假）\n");
        }

        // 判断星期几
        try {
            LocalDate parsed = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            result.append("星期: ").append(weekDays[parsed.getDayOfWeek().getValue() - 1]);
        } catch (Exception ignored) {
        }

        return result.toString();
    }

    @Tool(description = "查询指定日期范围内的所有法定节假日。用于旅行规划时避开或利用节假日出行")
    public String getHolidaysInRange(
            @ToolParam(description = "开始日期，格式YYYY-MM-DD") String startDate,
            @ToolParam(description = "结束日期，格式YYYY-MM-DD") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

            StringBuilder result = new StringBuilder();
            result.append("日期范围: ").append(startDate).append(" 至 ").append(endDate).append("\n");

            boolean found = false;
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (holidays.containsKey(dateStr)) {
                    found = true;
                    String holidayInfo = holidays.get(dateStr);
                    String[] parts = holidayInfo.split(",");
                    String name = parts.length >= 2 ? parts[1] : holidayInfo;
                    String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    String weekDay = weekDays[date.getDayOfWeek().getValue() - 1];
                    result.append("- ").append(dateStr).append(" (").append(weekDay).append("): ").append(name).append("\n");
                }
            }

            if (!found) {
                result.append("该日期范围内没有法定节假日\n");
            }

            // 同时标注范围内的调休日
            StringBuilder workdayNote = new StringBuilder();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (workdays.containsKey(dateStr)) {
                    workdayNote.append("- ").append(dateStr).append(" 为调休上班日\n");
                }
            }
            if (workdayNote.length() > 0) {
                result.append("\n⚠️ 调休提醒（这些日期需上班）:\n").append(workdayNote);
            }

            return result.toString();
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询距离指定日期最近的即将到来的法定节假日")
    public String getUpcomingHolidays(
            @ToolParam(description = "参考日期，格式YYYY-MM-DD，不传则为今天") String fromDate) {
        try {
            LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
            // 查找未来 90 天内的节假日
            LocalDate end = from.plusDays(90);

            StringBuilder result = new StringBuilder();
            result.append("从 ").append(fromDate).append(" 起未来90天内的节假日:\n");

            // 按日期排序分组，将连续日期合并为一个假期
            String currentHoliday = null;
            LocalDate holidayStart = null;
            LocalDate holidayEnd = null;
            int count = 0;

            for (LocalDate date = from; !date.isAfter(end); date = date.plusDays(1)) {
                String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String holidayInfo = holidays.get(dateStr);

                if (holidayInfo != null) {
                    String[] parts = holidayInfo.split(",");
                    String name = parts.length >= 2 ? parts[1] : holidayInfo;

                    if (!name.equals(currentHoliday)) {
                        // 输出上一个假期
                        if (currentHoliday != null) {
                            result.append(formatHolidayRange(currentHoliday, holidayStart, holidayEnd));
                            count++;
                        }
                        currentHoliday = name;
                        holidayStart = date;
                    }
                    holidayEnd = date;
                } else {
                    // 不在假期内
                    if (currentHoliday != null) {
                        result.append(formatHolidayRange(currentHoliday, holidayStart, holidayEnd));
                        count++;
                        currentHoliday = null;
                    }
                }
            }
            // 最后一个假期
            if (currentHoliday != null) {
                result.append(formatHolidayRange(currentHoliday, holidayStart, holidayEnd));
                count++;
            }

            if (count == 0) {
                result.append("未来90天内没有法定节假日\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    private String formatHolidayRange(String name, LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return "- " + name + ": " + start + "\n";
        }
        long days = end.toEpochDay() - start.toEpochDay() + 1;
        return "- " + name + ": " + start + " 至 " + end + "（共 " + days + " 天）\n";
    }
}
