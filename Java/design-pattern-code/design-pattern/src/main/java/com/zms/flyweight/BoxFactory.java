package com.zms.flyweight;

import java.util.HashMap;

// 工厂类
public class BoxFactory {

    private final HashMap<String, AbstractBox> map;

    public BoxFactory() {
        map = new HashMap<>();
        map.put("I", new IBox());
        map.put("O", new OBox());
        map.put("L", new LBox());
    }

    // 根据名称获取对象
    public AbstractBox getBox(String type) {
        return map.get(type);
    }

    public static BoxFactory getFactory() {
        return factory;
    }
    private static final BoxFactory factory = new BoxFactory();
}
