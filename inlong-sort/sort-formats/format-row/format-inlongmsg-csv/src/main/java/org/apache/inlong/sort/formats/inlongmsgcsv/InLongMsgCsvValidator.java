/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.inlongmsgcsv;

import org.apache.inlong.sort.formats.base.TextFormatDescriptorValidator;

import org.apache.flink.table.descriptors.DescriptorProperties;

import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_DELIMITER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_LINE_DELIMITER;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.validateInLongMsgSchema;
import static org.apache.inlong.sort.formats.inlongmsgcsv.InLongMsgCsvUtils.FORMAT_DELETE_HEAD_DELIMITER;

/**
 * The validator for {@link InLongMsgCsv}.
 */
public class InLongMsgCsvValidator extends TextFormatDescriptorValidator {

    @Override
    public void validate(DescriptorProperties properties) {
        super.validate(properties);

        properties.validateString(FORMAT_DELIMITER, true, 1, 1);
        properties.validateString(FORMAT_LINE_DELIMITER, true, 1, 1);
        properties.validateBoolean(FORMAT_DELETE_HEAD_DELIMITER, true);

        validateInLongMsgSchema(properties);
    }
}
