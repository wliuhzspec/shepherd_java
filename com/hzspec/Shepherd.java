package com.hzspec;

import java.io.*;
import java.net.*;
import java.util.*;


public class Shepherd{
	public static void main(String[] args) throws Exception{
		
		String apMacAddr = getApMac();
		
        Properties prop = new Properties();
        FileInputStream iniConfig = new FileInputStream("ini.cfg");
        prop.load(iniConfig);
        iniConfig.close();

		String userRegUrl = prop.getProperty("UserRegURL");
        String userRegParam = getUserRegParam();

        //register user
		sendPost(userRegUrl, userRegParam);


        //query periodically
		while(true){
			Thread.sleep(5000);
            String queryUrl = prop.getProperty("QueryURL");

			StringBuffer queryParam = new StringBuffer();
            queryParam.append("apMacAddr=");
            queryParam.append(apMacAddr);

			String queryResp = sendGet(queryUrl, queryParam.toString());
			if (!"".equals(queryResp) ) {
				handleQueryResponse(queryResp);
			}
			
		}	
	}

    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            
            URL realUrl = new URL(urlNameString);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
			connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("charsets", "utf-8");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();
            Map<String, List<String>> map = connection.getHeaderFields();
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
		
			int rspCode = connection.getResponseCode();
			if (rspCode == 200){

				in = new BufferedReader(new InputStreamReader(
							connection.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					result += line;
				}
			}
        } catch (Exception e) {
            System.out.println("send GET request exceptin ！" + e);
            e.printStackTrace();
        }
        // close input stream
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
        return result;
    }

    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            URLConnection conn = realUrl.openConnection();
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = new PrintWriter(conn.getOutputStream());
            out.print(param);
            out.flush();
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("send POST exception！"+e);
            e.printStackTrace();
        }
        // close input stream
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }    

	public static void handleQueryResponse(String resp){

		int ruleEndIndex = resp.indexOf(',');
		List<String>  addToBlackList = new ArrayList<String> ();
		List<String>  removeFromBlackList = new ArrayList<String> ();

		while (ruleEndIndex != -1 ){
			String iptableEntry = resp.substring(0,ruleEndIndex);
			resp.replace(iptableEntry, "");
			ruleEndIndex = resp.indexOf(',');

			int dashIndex = iptableEntry.indexOf('-');
			if (dashIndex != -1) {

				String macAddr = iptableEntry.substring(0, dashIndex);
				char opCode = iptableEntry.charAt(dashIndex + 1 );

				switch (opCode) {

					case '0':
						//to update iptables, drop
						//add to black list
						addToBlackList.add(macAddr);
						break;
					case '1':
						//to update iptables, accept
						//remove from black list
						removeFromBlackList.add(macAddr);
						break;
					case '3':
						//to report device status
                        try {
                            reportLANHostInfo();
                        }
                        catch (Exception e){
                            System.out.println("report active host exceptin ！" + e);
                            e.printStackTrace();

                        }

                        break;

				}	

			}

		}
		String[] addToBlackArray = new String[addToBlackList.size()];
		String[] removeFromBlackArray = new String[removeFromBlackList.size()];
		//addLANHostToBlackList(addToBlackArray);
		//deleteLANHostFromBlackList(removeFromBlackArray);
	}

	public static void reportLANHostInfo() throws Exception {
	
        try {
            Properties prop = new Properties();
            FileInputStream iniConfig = new FileInputStream("ini.cfg");
            prop.load(iniConfig);
            iniConfig.close();

            String activeHostReportUrl = prop.getProperty("ActiveHostReportURL");
            String hostInfo = getHostInfo();  
            if (hostInfo != null) {
                sendPost(activeHostReportUrl, hostInfo);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
	}


    public static String getUserRegParam() {
    
        
        Map <String, String> testUserRegParam = new HashMap <String, String> ();
        testUserRegParam.put("userId", "test");
        testUserRegParam.put("passwd", "test" );
        testUserRegParam.put("phoneNumber", "1234");
        testUserRegParam.put("emailAddr", "test@test.com");
        testUserRegParam.put("ip", "60.205.212.99");
        testUserRegParam.put("apMacAddr", "e8:4e:06:47:77:8c");

			
		StringBuffer userRegParam = new StringBuffer ();
        //Set entrySet = testUserRegParam.entrySet();

        userRegParam.append("{\"userId\":\"");

        for (Map.Entry <String, String> entry : testUserRegParam.entrySet()) {
            userRegParam.append("\"");
            userRegParam.append(entry.getKey());
            userRegParam.append("\"");
    
            userRegParam.append(":");


            userRegParam.append("\"");
            userRegParam.append(entry.getValue());
            userRegParam.append("\"");
        
            userRegParam.append(",");
        }

        userRegParam.append("}");

        return userRegParam.toString();
    
    }


    public static String getApMac() {
    
		String apMacAddr = "e8:4e:06:47:77:8c";
        return apMacAddr.replaceAll(":", "%3A");
    }


    public static String getHostInfo() {
    
		//String hostInfo = getLANHostInfoByClass("ONLINE");
		String apMacAddr = getApMac();
		String hostInfo =  "{ \"Result\":0,  \"List\": [{ \"Class\":ONLINE, \"MAC\":\"00:01:02:03:04:05\", \"DhcpName\":\"client1\", \"DeviceName\":\"client1\", \"IP\":\"192.168.100.100\", \"DevType”：\"PC\", \"ConnectInterface \":\"LAN1\", \"OnlineTime \":\"100\", \"StorageAccessStatus\":\"ON\" } ] }";
		//String apMacAddr = "e8:4e:06:47:77:8c";

		// getHostInfo return error
		if(!hostInfo.contains(" \"Result\":0")) {
			System.out.println("getHostInfo return code != 0");
			return null;
		}
		else{
			String apMacField = "\"apMacAddr\":\"";
			apMacField += apMacAddr;
			apMacField +="\"";
			hostInfo = hostInfo.replace("\"Result\":0", apMacField);
			hostInfo = hostInfo.replace("List", "activeDevices");

			hostInfo = hostInfo.replaceAll("MAC", "deviceMacAddr");
			hostInfo = hostInfo.replaceAll("IP", "deviceIp");

            return hostInfo;
        }

    
    }


}
