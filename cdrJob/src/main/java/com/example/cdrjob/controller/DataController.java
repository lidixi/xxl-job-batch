package com.example.cdrjob.controller;

import com.example.cdrjob.dao.DataDao;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data")
public class DataController {

    private final DataDao dataDao;

    public DataController(DataDao dataDao) {
        this.dataDao = dataDao;
    }

    @GetMapping("/records")
    public List<Map<String, Object>> getAllRecords() {
        return dataDao.getAllRecords();
    }

    @GetMapping("/wrong-records/{wrongIds}")
    public List<Map<String, Object>> getWrongRecords(@PathVariable String wrongIds) {
        return dataDao.getWrongRecords(wrongIds);
    }

    @GetMapping("/recent-rates/{rule}")
    public List<Map<String, Object>> getRecentRates(@PathVariable String rule) {
        return dataDao.getRecentRates(rule);
    }

    @GetMapping("/group-stats/{group}")
    public Map<String, Object> getGroupStats(@PathVariable String group) {
        return dataDao.getGroupStats(group);
    }
}