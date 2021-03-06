/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query.types.expr;

import java.util.Date;

import javax.annotation.Nullable;

import com.mysema.query.types.Ops;
import com.mysema.query.types.Path;
import com.mysema.query.types.PathImpl;

/**
 * TimeExpression represents Time expressions
 *
 * @author tiwe
 *
 * @param <T> expression type
 */
@SuppressWarnings({"unchecked"})
public abstract class TimeExpression<T extends Comparable> extends TemporalExpression<T> {

    private static final long serialVersionUID = 7360552308332457990L;

    private static final TimeExpression<Date> CURRENT_TIME = currentTime(Date.class);

    @Nullable
    private volatile NumberExpression<Integer> hours, minutes, seconds, milliseconds;

    public TimeExpression(Class<? extends T> type) {
        super(type);
    }

    @Override
    public TimeExpression<T> as(Path<T> alias) {
        return TimeOperation.create((Class<T>)getType(),Ops.ALIAS, this, alias);
    }

    @Override
    public TimeExpression<T> as(String alias) {
        return as(new PathImpl<T>(getType(), alias));
    }

    /**
     * Get a hours expression (range 0-23)
     *
     * @return
     */
    public NumberExpression<Integer> hour(){
        if (hours == null) {
            hours = NumberOperation.create(Integer.class, Ops.DateTimeOps.HOUR, this);
        }
        return hours;
    }

    /**
     * Get a minutes expression (range 0-59)
     *
     * @return
     */
    public NumberExpression<Integer> minute(){
        if (minutes == null) {
            minutes = NumberOperation.create(Integer.class, Ops.DateTimeOps.MINUTE, this);
        }
        return minutes;
    }

    /**
     * Get a seconds expression (range 0-59)
     *
     * @return
     */
    public NumberExpression<Integer> second(){
        if (seconds == null) {
            seconds = NumberOperation.create(Integer.class, Ops.DateTimeOps.SECOND, this);
        }
        return seconds;
    }

    /**
     * Get a milliseconds expression (range 0-999)
     * <p>Is always 0 in HQL and JDOQL modules</p>
     *
     * @return
     */
    public NumberExpression<Integer> milliSecond(){
        if (milliseconds == null) { 
            milliseconds = NumberOperation.create(Integer.class, Ops.DateTimeOps.MILLISECOND, this);
        }
        return milliseconds;
    }

    /**
     * Get an expression representing the current time as a ETime instance
     *
     * @return
     */
    public static TimeExpression<Date> currentTime() {
        return CURRENT_TIME;
    }

    /**
     * Get an expression representing the current time as a ETime instance
     *
     * @return
     */
    public static <T extends Comparable> TimeExpression<T> currentTime(Class<T> cl) {
        return TimeOperation.create(cl, Ops.DateTimeOps.CURRENT_TIME);
    }

}
