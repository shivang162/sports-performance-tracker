package com.tracker.service;

public class PerformanceService {

    public double calculateScore(double speed,double accuracy,double stamina){

        double score = (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);

        return score;
    }
}
