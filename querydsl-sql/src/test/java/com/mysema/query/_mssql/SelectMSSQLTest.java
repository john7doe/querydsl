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
package com.mysema.query._mssql;

import static com.mysema.query.Constants.employee;
import static com.mysema.query.Constants.employee2;
import static com.mysema.query.sql.mssql.SQLServerGrammar.rn;
import static com.mysema.query.sql.mssql.SQLServerGrammar.rowNumber;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mysema.query.Connections;
import com.mysema.query.SelectBaseTest;
import com.mysema.query.Target;
import com.mysema.query.sql.SQLServerTemplates;
import com.mysema.query.sql.mssql.RowNumber;
import com.mysema.query.types.Expression;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.query.ListSubQuery;
import com.mysema.query.types.query.SimpleSubQuery;
import com.mysema.query.types.template.SimpleTemplate;
import com.mysema.testutil.Label;
import com.mysema.testutil.ResourceCheck;

@ResourceCheck("/sqlserver.run")
@Label(Target.SQLSERVER)
public class SelectMSSQLTest extends SelectBaseTest {

    @BeforeClass
    public static void setUp() throws Exception {
        Connections.initSQLServer();
    }

    @Before
    public void setUpForTest() {
        templates = new SQLServerTemplates(){{
            newLineToSingleSpace();
        }};
    }

    @Test
    public void manualPaging(){
        RowNumber rowNumber = rowNumber().orderBy(employee.lastname.asc()).as(rn);
        // TODO : create a short cut for wild card
        Expression<Object[]> all = SimpleTemplate.create(Object[].class, "*");

        // simple
        System.out.println("#1");
        for (Object[] row : query().from(employee).list(employee.firstname, employee.lastname, rowNumber)){
            System.out.println(Arrays.asList(row));
        }
        System.out.println();

        // with subquery, generic alias
        System.out.println("#2");
        ListSubQuery<Object[]> sub = sq().from(employee).list(employee.firstname, employee.lastname, rowNumber);
        SimplePath<Object[]> subAlias = new SimplePath<Object[]>(Object[].class, "s");
        for (Object[] row : query().from(sub.as(subAlias)).list(all)){
            System.out.println(Arrays.asList(row));
        }
        System.out.println();

        // with subquery, only row number
        System.out.println("#3");
        SimpleSubQuery<Long> sub2 = sq().from(employee).unique(rowNumber);
        SimplePath<Long> subAlias2 = new SimplePath<Long>(Long.class, "s");
        for (Object[] row : query().from(sub2.as(subAlias2)).list(all)){
            System.out.println(Arrays.asList(row));
        }
        System.out.println();

        // with subquery, specific alias
        System.out.println("#4");
        ListSubQuery<Object[]> sub3 = sq().from(employee).list(employee.firstname, employee.lastname, rowNumber);
        for (Object[] row : query().from(sub3.as(employee2)).list(employee2.firstname, employee2.lastname)){
            System.out.println(Arrays.asList(row));
        }
    }

}
