package com.kiro.metadata.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 目录层级超限异常
 * 当创建的目录层级超过最大限制时抛出
 * 
 * @author Kiro
 */
public class MaxLevelExceededException extends BusinessException {
    
    /**
     * 构造目录层级超限异常
     * 
     * @param currentLevel 当前层级
     * @param maxLevel 最大层级
     */
    public MaxLevelExceededException(int currentLevel, int maxLevel) {
        super("MAX_LEVEL_EXCEEDED", 
              String.format("目录层级不能超过 %d 级,当前为 %d 级", maxLevel, currentLevel), 
              HttpStatus.BAD_REQUEST,
              Map.of("current_level", currentLevel, "max_level", maxLevel));
    }
    
    /**
     * 构造目录层级超限异常(默认最大层级为5)
     * 
     * @param currentLevel 当前层级
     */
    public MaxLevelExceededException(int currentLevel) {
        this(currentLevel, 5);
    }
}
