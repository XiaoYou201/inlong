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

package org.apache.inlong.sort.formats.inlongmsgtlogkv;

import org.apache.inlong.common.pojo.sort.dataflow.field.format.FormatInfo;
import org.apache.inlong.common.pojo.sort.dataflow.field.format.RowFormatInfo;
import org.apache.inlong.sort.formats.base.FieldToRowDataConverters.FieldToRowDataConverter;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgBody;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgHead;

import org.apache.flink.table.data.GenericRowData;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.formats.base.TableFormatUtils.deserializeBasicField;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.INLONGMSG_ATTR_TIME_DT;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.INLONGMSG_ATTR_TIME_T;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.getPredefinedFields;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.parseAttr;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.parseDateTime;
import static org.apache.inlong.sort.formats.inlongmsg.rowdata.InLongMsgUtils.parseEpochTime;
import static org.apache.inlong.sort.formats.util.StringUtils.splitCsv;
import static org.apache.inlong.sort.formats.util.StringUtils.splitKv;

/**
 * Utilities for {@link InLongMsgTlogKv}.
 */
public class InLongMsgTlogKvUtils {

    public static final String DEFAULT_INLONGMSG_TLOGKV_CHARSET = "ISO_8859_1";

    public static InLongMsgHead parseHead(String attr) {
        Map<String, String> attributes = parseAttr(attr);

        Timestamp time;
        if (attributes.containsKey(INLONGMSG_ATTR_TIME_T)) {
            String date = attributes.get(INLONGMSG_ATTR_TIME_T).trim();
            time = parseDateTime(date);
        } else if (attributes.containsKey(INLONGMSG_ATTR_TIME_DT)) {
            String epoch = attributes.get(INLONGMSG_ATTR_TIME_DT).trim();
            time = parseEpochTime(epoch);
        } else {
            throw new IllegalArgumentException(
                    "Could not find " + INLONGMSG_ATTR_TIME_T +
                            " or " + INLONGMSG_ATTR_TIME_DT + " in attributes!");
        }

        List<String> predefinedFields = getPredefinedFields(attributes);

        return new InLongMsgHead(attributes, null, time, predefinedFields);
    }

    public static InLongMsgBody parseBody(
            byte[] bytes,
            String charset,
            char delimiter,
            char entryDelimiter,
            char kvDelimiter,
            Character escapeChar,
            Character quoteChar) {
        String text;
        if (bytes[0] == delimiter) {
            text = new String(bytes, 1, bytes.length - 1, Charset.forName(charset));
        } else {
            text = new String(bytes, Charset.forName(charset));
        }

        String[] segments = splitCsv(text, delimiter, escapeChar, quoteChar);

        String streamId = segments[0];

        Map<String, String> entries;
        if (segments.length > 1) {
            entries = splitKv(segments[1], entryDelimiter, kvDelimiter, escapeChar, quoteChar);
        } else {
            entries = Collections.emptyMap();
        }

        return new InLongMsgBody(bytes, streamId, Collections.emptyList(), entries);
    }

    /**
     * Deserializes the row from the given entries.
     *
     * @param rowFormatInfo The format information of the row.
     * @param nullLiteral The literal for null values.
     * @param predefinedFields The predefined fields.
     * @param entries The entries.
     * @return The row deserialized from the given entries.
     */
    public static GenericRowData deserializeRowData(
            RowFormatInfo rowFormatInfo,
            String nullLiteral,
            List<String> predefinedFields,
            Map<String, String> entries,
            FieldToRowDataConverter[] converters) {
        String[] fieldNames = rowFormatInfo.getFieldNames();
        FormatInfo[] fieldFormatInfos = rowFormatInfo.getFieldFormatInfos();

        GenericRowData rowData = new GenericRowData(fieldNames.length);

        for (int i = 0; i < predefinedFields.size(); ++i) {

            if (i >= fieldNames.length) {
                break;
            }

            String fieldName = fieldNames[i];
            FormatInfo fieldFormatInfo = fieldFormatInfos[i];
            String fieldText = predefinedFields.get(i);
            FieldToRowDataConverter converter = converters[i];
            Object field = converter.convert(
                    deserializeBasicField(
                            fieldName,
                            fieldFormatInfo,
                            fieldText,
                            nullLiteral));
            rowData.setField(i, field);
        }

        for (int i = predefinedFields.size(); i < fieldNames.length; ++i) {
            String fieldName = fieldNames[i];
            FormatInfo fieldFormatInfo = fieldFormatInfos[i];
            String fieldText = entries.get(fieldName);
            FieldToRowDataConverter converter = converters[i];
            Object field = converter.convert(deserializeBasicField(
                    fieldName,
                    fieldFormatInfo,
                    fieldText,
                    nullLiteral));
            rowData.setField(i, field);
        }

        return rowData;
    }

}
