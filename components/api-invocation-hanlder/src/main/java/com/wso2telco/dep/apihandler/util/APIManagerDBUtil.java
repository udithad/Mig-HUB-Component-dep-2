/*******************************************************************************
 * Copyright (c) 2015-2017, WSO2.Telco Inc. (http://www.wso2telco.com)
 *
 * All Rights Reserved. WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.apihandler.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wso2telco.core.dbutils.DbUtils;
import com.wso2telco.core.dbutils.util.DataSourceNames;
import com.wso2telco.dep.apihandler.dto.TokenDTO;

public class APIManagerDBUtil {
	private static final Log log = LogFactory.getLog(APIManagerDBUtil.class);

	private APIManagerDBUtil() {
	}

	public static TokenDTO getTokenDetailsFromAPIManagerDB(String consumerKey) {
		TokenDTO tokenDto = new TokenDTO();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DbUtils.getDbConnection(DataSourceNames.WSO2AM_DB);
			StringBuilder queryString = new StringBuilder("SELECT it.ACCESS_TOKEN ");
			queryString.append("FROM idn_oauth_consumer_apps cp ");
			queryString.append("inner join idn_oauth2_access_token it ");
			queryString.append("on it.CONSUMER_KEY_ID = cp.ID ");
			queryString.append("where cp.CONSUMER_KEY= ?  ");
			queryString.append("AND it.TOKEN_STATE='ACTIVE' ");
			queryString.append("AND it.AUTHZ_USER= cp.USERNAME");

			ps = con.prepareStatement(queryString.toString());
			ps.setString(1, consumerKey);
			log.debug("sql query in APIManagerDBUtil : " + ps);
			rs = ps.executeQuery();
			rs.next();
			tokenDto.setAccessToken(rs.getString(1));
		} catch (SQLException e) {
			log.error("database operation error in APIManagerDBUtil : ", e);
		} catch (Exception e) {
			log.error("error in APIManagerDBUtil : ", e);
		} finally {
			DbUtils.closeAllConnections(ps, con, rs);
		}
		return tokenDto;
	}
}
