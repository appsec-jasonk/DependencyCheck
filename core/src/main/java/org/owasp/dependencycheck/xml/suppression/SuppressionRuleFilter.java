/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2021 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.xml.suppression;

/**
 * A simple suppression rule filter.
 *
 * @author Jeremy Long
 */
public interface SuppressionRuleFilter {

    /**
     * Determines whether or not to filter a suppression rule.
     *
     * @param rule the suppression rule to evaluate
     * @return <code>true</code> if the rule should be filtered; otherwise
     * <code>true</code>
     */
    public boolean filter(SuppressionRule rule);

    /**
     * Returns the name of the filter.
     *
     * @return the name of the filters
     */
   String getName();
}
