# xxl-job-batch
不知道能不能运行，没有报错。

这个SpringBoot项目与XXL-JOB集成，利用XXL-JOB的任务调度功能来执行具体规则和记录日志，并使用JDBCTemplate操作MySQL完成我的业务逻辑部分。
xxl-job执行器的AppName为“CDR”，在执行器下有一个通用任务，其JobHander为“CDR”，用户在xxl-job的前端界面里创建该定时任务时设置好执行器和JobHandler外，还会设置任务描述为“CDR资源库质量监测任务”、设置Cron表达式和任务参数，其中任务参数包含了所要执行的具体规则的ID和SQL子模版的筛选参数，决定了此任务执行时在业务逻辑部分中要调用哪些具体规则和这些具体规则SQL子模版的筛选参数。

在业务逻辑部分中有2个模块，分别为规则维护模块和规则执行模块：
1.在规则维护模块中，预设有3个规则类型分别是完整性、数值范围和唯一性，每个规则类型包含3个SQL父模版，分别对应基础值、稽查值和异常值的查询，其规则类型和具体规则储存在BUILD库中，规则类型储存在GROUP表，字段有主键ID、规则类型GROUP_NAME、基础值查询SQL父模版BASE_PARENT、稽查值查询SQL父模版AUDIT_PARENT、异常值查询SQL父模版WRONG_PARENT。用户选择规则类型来新建其具体规则时，规则类型的3个SQL父模版会拼接数据库、表、字段等数据库参数生成新建具体规则的3个SQL子模版，具体规则创建后储存在RULE表，字段有主键ID、对应规则类型ID的外键GROUP_ID、具体规则RULE_NAME、基础值查询SQL子模版BASE_SON、稽查值查询SQL子模版AUDIT_SON、异常值查询SQL子模版WRONG_SON。要求在创建具体规则的过程中，查询本地MySQL数据库里有哪些库，用户选择数据库后会返回数据库里的所有表，用户选择表后会返回表中的所有字段，用户选择好所有数据参数后，根据规则类型的3个SQL父模版生成该具体规则的3个SQL子模版。用户将该具体规则命名后，规则模块会把该具体规则的信息及其3个SQL子模版持久化保存在数据库RULE中，并支持修改更新其数据库、表、字段等参数后修改更新其SQL子模版，在该具体规则被调用时，根据其数据库、表、字段信息选择数据源并读取其3个SQL子模版。数据库参数为数据源用户user、数据库database、表table、字段field,筛选参数为院区hospital、开始日期startDate、结束日期endDate（特别的，数值范围规则类型的SQL模版还有最大值maxValue和最小值minValue参数，且属于筛选参数；特别的，唯一性的SQL模版需要2个字段参数分别为field1和field2）。SQL父模版已储存在GROUP表中。

2.在执行模块会操作多个数据源，BUILD库中的RULE表储存具体规则及其SQL子模版，CDR_BASE库中的基础表PATIENT_BASE数据需要被稽查，CDR_AUDIT库中的稽查表PATIENT_AUDIT数据作为标准，LOG库有记录数据统计情况的数据日志表DATA，动态数据源拦截器负责会话多个数据库。读取xxl-job定时任务时的任务参数后，会根据所要执行的具体规则ID来选择此次任务要执行的具体规则，根据筛选参数来拼接SQL子模版成为可执行SQL，并依照xxl-job的调度执行。
PATIENT_BASE表和PATIENT_AUDIT表的字段均为主键ID、就诊号PATIENT_ID、姓名NAME、身份证号ID_NO、出生日期BIRTH_DATE、创建日期CREATE_DATE、年龄AGE、院区YQ_NAME；
DATA表的字段为主键ID、任务TASK、具体规则RULE、规则类型GROUP_NAME(根据RLUE表的外键查GROUP表的命名)、基础值BASE_VALUE、稽查值AUDIT_VALUE、异常值WRONG_VALUE、比例RATE（基础值/稽查值%）、监测时间范围TIME_RANGE、运行时间RUN_TIME、异常记录的ID统计WRONG_ID（格式为1，2，3，...等）；
执行模块还需要这些查询接口，分别为：查询DATA表所有记录；根据WRONG_ID里的所有ID查询PATIENT_BASE表的记录；根据具体规则RULE查询DATA表中近15次RULE相符记录中的AUDIT_VALUE和RATE两个字段；根据规则类型GROUP查询DATA表中GROUP相符的记录后，计算其、BASE_VALUE的基础值总和及WRONG_VALUE的异常值总和，返回这两个总和，以及返回[（基础值总和-异常值总和）/基础值总和]%的正常值比例。

模版：完整性：
基础值查询：baseQuery = "SELECT COUNT(*) FROM " + table + " WHERE 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "");
稽查值查询：auditQuery = "SELECT COUNT(*) FROM " + table + " WHERE " + field + " IS NOT NULL AND 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "");
异常值查询： exceptionQuery = "SELECT * FROM " + table + " WHERE " + field + " IS NULL AND 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "");

数值范围：
基础值查询：baseQuery = "SELECT COUNT(*) FROM " + table + " WHERE 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "");
稽查值查询：auditQuery = "SELECT COUNT(*) FROM " + table + " WHERE 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "") +
            " AND " + field + " >= " + minValue + " AND " + field + " <= " + maxValue;
异常值查询：exceptionQuery = "SELECT * FROM " + table + " WHERE 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "") +
            " AND (" + field + " < " + minValue + " OR " + field + " > " + maxValue + ")";

唯一性：
基础值查询：baseQuery = "SELECT COUNT(*) FROM " + table + " WHERE 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "");
稽查值查询：auditQuery = "SELECT COUNT(*) FROM (SELECT " + field1 + ", " + field2 + " FROM " + table +
            " WHERE ID_NO IS NOT NULL AND 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "") +
            " GROUP BY PATIENT_ID HAVING COUNT(" + field1 + ", " + field2 + ") = 1)";
异常值查询：exceptionQuery = "SELECT B.* FROM " + table + " B, (SELECT " + field1 + ", " + field2 + 
            " FROM " + table + " WHERE ID_NO IS NOT NULL AND 1=1" +
            (hospital != null && !hospital.isEmpty() ? " AND YQ_NAME IN (" + hospital + ")" : "") +
            (startDate != null && !startDate.isEmpty() ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "") +
            (endDate != null && !endDate.isEmpty() ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "") +
            " GROUP BY PATIENT_ID HAVING COUNT(" + field1 + ", " + field2 + ") > 1) A" +
            " WHERE A." + field1 + " = B." + field1 + " AND A." + field2 + " = B." + field2;

数据库
CREATE TABLE `GROUP` (
    `ID` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `GROUP_NAME` VARCHAR(255) NOT NULL,
    `BASE_PARENT` TEXT NOT NULL,
    `AUDIT_PARENT` TEXT NOT NULL,
    `WRONG_PARENT` TEXT NOT NULL
);

CREATE TABLE `RULE` (
    `ID` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `GROUP_ID` BIGINT NOT NULL,
    `RULE_NAME` VARCHAR(255) NOT NULL,
    `BASE_SON` TEXT NOT NULL,
    `AUDIT_SON` TEXT NOT NULL,
    `WRONG_SON` TEXT NOT NULL,
    FOREIGN KEY (`GROUP_ID`) REFERENCES `GROUP`(`ID`)
);

CREATE TABLE `DATA` (
    `ID` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `TASK` VARCHAR(255) NOT NULL,
    `RULE` BIGINT NOT NULL,
    `GROUP_NAME` VARCHAR(255) NOT NULL,
    `BASE_VALUE` INT NOT NULL,
    `AUDIT_VALUE` INT NOT NULL,
    `WRONG_VALUE` INT NOT NULL,
    `RATE` DOUBLE NOT NULL,
    `TIME_RANGE` VARCHAR(255) NOT NULL,
    `RUN_TIME` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `WRONG_ID` TEXT
);

CREATE TABLE `PATIENT_BASE` (
    `ID` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `PATIENT_ID` VARCHAR(255) NOT NULL,
    `NAME` VARCHAR(255) NOT NULL,
    `ID_NO` VARCHAR(255) NOT NULL,
    `BIRTH_DATE` DATE,
    `CREATE_DATE` DATE,
    `AGE` INT,
    `YQ_NAME` VARCHAR(255)
);

CREATE TABLE `PATIENT_AUDIT` (
    `ID` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `PATIENT_ID` VARCHAR(255) NOT NULL,
    `NAME` VARCHAR(255) NOT NULL,
    `ID_NO` VARCHAR(255) NOT NULL,
    `BIRTH_DATE` DATE,
    `CREATE_DATE` DATE,
    `AGE` INT,
    `YQ_NAME` VARCHAR(255)
);


sb垃圾公司cnm，月薪3000我啥都不会，叫我写这玩意，写不出来不给转正，我还不如去商场当服务员
