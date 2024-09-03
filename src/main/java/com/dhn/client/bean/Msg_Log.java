package com.dhn.client.bean;

import lombok.Data;

@Data
public class Msg_Log {
	private String at_table;
	private String at_log_table;
	private String msg_table;
	private String msg_log_table;
	private String msgid;
	private String code;
	private String s_code;
	private String result_dt;
	private String telecom;
	private String status;
	private String real_send_type;
	private String database;
}
