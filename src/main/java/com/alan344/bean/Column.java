package com.alan344.bean;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author AlanSun
 * @date 2019/8/12 13:43
 */
@Getter
@Setter
public class Column {
    private String columnName;
    private String type;
    private BooleanProperty ignore = new SimpleBooleanProperty(false);

    public boolean isIgnore() {
        return ignore.get();
    }

    public BooleanProperty ignoreProperty() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore.set(ignore);
    }
}
