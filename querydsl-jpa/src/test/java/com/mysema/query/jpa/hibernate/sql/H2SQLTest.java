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
package com.mysema.query.jpa.hibernate.sql;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.mysema.query.sql.H2Templates;
import com.mysema.testutil.HibernateConfig;
import com.mysema.testutil.HibernateTestRunner;

@RunWith(HibernateTestRunner.class)
@HibernateConfig("h2.properties")
public class H2SQLTest extends AbstractSQLTest{
    
    public H2SQLTest() {
        super(new H2Templates());
    }
    
    @Test
    public void Count_Via_Unique(){
        // 
    }    
    
    @Test
    public void List_With_Offset(){
        // 
    }
    
    @Test
    public void Wildcard(){
        // 
    }


}
