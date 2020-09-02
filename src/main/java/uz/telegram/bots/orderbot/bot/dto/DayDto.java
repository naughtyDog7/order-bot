package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;

import java.time.LocalTime;

@Getter
@Setter
public class DayDto {
    @JsonProperty("day_code")
    private int dayCode;
    @JsonProperty("open_time")
    private String openTime;
    @JsonProperty("close_time")
    private String closeTime;
    @JsonProperty("day_off")
    private boolean dayOff;
    @JsonProperty("timetable_code")
    private int timetableCode;

    public static WorkingTime toWorkingTime(DayDto dayDto) {
        return new WorkingTime(dayDto.dayOff, LocalTime.parse(dayDto.openTime), LocalTime.parse(dayDto.closeTime));
    }
}
