package com.tracker.service;

public class PerformanceLevel {

    public String getLevel(double score){

        if(score >= 85)
            return "Excellent";

        else if(score >= 70)
            return "Good";

        else if(score >= 50)
            return "Average";

        else
            return "Needs Improvement";
    }
}
