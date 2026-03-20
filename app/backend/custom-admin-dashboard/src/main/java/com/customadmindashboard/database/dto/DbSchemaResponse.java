package com.customadmindashboard.database.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbSchemaResponse {
    private String tableName;
    private List<ColumnInfo> columns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private boolean isNullable;
        private boolean isPrimaryKey;
        private boolean isForeignKey;
        private String referencedTable;
    }
}
