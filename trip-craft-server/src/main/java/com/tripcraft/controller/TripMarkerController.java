package com.tripcraft.controller;

import com.tripcraft.entity.TripMarker;
import com.tripcraft.service.TripMarkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 打卡点接口.
 */
@RestController
@RequestMapping("/api/markers")
@RequiredArgsConstructor
public class TripMarkerController {

    private final TripMarkerService tripMarkerService;

    /**
     * 查询所有打卡点，按创建时间倒序返回.
     */
    @GetMapping
    public List<TripMarker> listMarkers() {
        return tripMarkerService.listMarkers();
    }

    /**
     * 保存一个新的打卡点.
     *
     * @param marker 请求体 JSON，如 {"title":"外滩","longitude":121.4903,"latitude":31.2397,"notes":"夜景"}
     * @return 已保存的打卡点（含自增 id 与创建时间），HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripMarker createMarker(@RequestBody TripMarker marker) {
        return tripMarkerService.createMarker(marker);
    }
}
