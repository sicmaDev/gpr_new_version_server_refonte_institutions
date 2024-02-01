package com.sicmagroup.gpr.domain.dto.reports;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RgbColor {
    private int r;
    private int g;
    private int b;

    public String toBgString(){
        return "rgba("+r+","+g+","+b+",0.5)";
    }

    public String toBorderString(){
        return "rgba("+r+","+g+","+b+",1)";
    }
}
