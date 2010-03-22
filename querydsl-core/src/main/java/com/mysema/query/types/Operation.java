/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.types;

import java.util.List;


/**
 * Operation represents an operation with operator and arguments
 * 
 * @author tiwe
 * @version $Id$
 */
public interface Operation<OP, RT> {
    /**
     * @return
     */
    Expr<RT> asExpr();

    /**
     * Get the argument with the given index
     * 
     * @param index
     * @return
     */
    Expr<?> getArg(int index);

    /**
     * Get the arguments of this operation
     * 
     * @return
     */
    List<Expr<?>> getArgs();

    /**
     * Get the operator symbol for this operation
     * 
     * @return
     */
    Operator<OP> getOperator();
    
    /**
     * Get the type of this operation
     * 
     * @return
     */
    Class<? extends RT> getType();

}