package com.hzspec;

import java.io.*;
import java.net.*;
import java.util.*;

//import com.chinaunicom.smartgateway.deviceservices.*

public class Shepherd{
	public static void main(String[] args) throws InterruptedException{
		
		//String apMacAddr = getDeviceMAC();
		String apMacAddr = "e8:4e:06:47:77:8c";
		//to register user and devices
		
		String usrRegUrl = "http://60.205.212.99/squirrel/v1/users";
		String userId = "test";
		String passWd = "test";
		String phoneNum = "123456";
		String email = "test@test.com";
		String wanIp = "60.205.212.99";

			
		String usrRegParam ="{\"userId\":\"";
		usrRegParam += userId;
		usrRegParam += "\"";

		usrRegParam += "\"password\":\"";
		usrRegParam += passWd;
		usrRegParam += "\"";

		usrRegParam += "\"phoneNumber\":\"";
		usrRegParam += phoneNum;
		usrRegParam += "\"";

		usrRegParam += "\"emailAddr\":\"";
		usrRegParam += email;
		usrRegParam += "\"";

		usrRegParam += "\"ip\":\"";
		usrRegParam += wanIp;
		usrRegParam += "\"";
		
		usrRegParam += "\"apMacAddr\":\"";
		usrRegParam += apMacAddr;
		usrRegParam += "\"}";

		sendPost(usrRegUrl, usrRegParam);

		while(true){
			Thread.sleep(5000);
			String queryUrl = "http://60.205.212.99/ext/v1/devices/ap_interval";
			String queryParm = "apMacAddr=";
			apMacAddr = apMacAddr.replaceAll(":", "%3A");
			queryParm += apMacAddr;

			String queryResp = sendGet(queryUrl, queryParm);
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
						reportLANHostInfo();
						break;

				}	

			}

		}
		String[] addToBlackArray = new String[addToBlackList.size()];
		String[] removeFromBlackArray = new String[removeFromBlackList.size()];
		//addLANHostToBlackList(addToBlackArray);
		//deleteLANHostFromBlackList(removeFromBlackArray);
	}

	public static void reportLANHostInfo() {
	
		//String hostInfo = getLANHostInfoByMac();
		
		//String hostInfo = getLANHostInfoByClass("ONLINE");
		//String apMacAddr = getDeviceMAC();
		String hostInfo =  "{ \"Result\":0,  \"List\": [{ \"Class\":ONLINE, \"MAC\":\"00:01:02:03:04:05\", \"DhcpName\":\"client1\", \"DeviceName\":\"client1\", \"IP\":\"192.168.100.100\", \"DevType”：\"PC\", \"ConnectInterface \":\"LAN1\", \"OnlineTime \":\"100\", \"StorageAccessStatus\":\"ON\" } ] }";
		String apMacAddr = "e8:4e:06:47:77:8c";

		// getHostInfo return error
		if(!hostInfo.contains(" \"Result\":0")) {
			System.out.println("getHostInfo return code != 0");
			return;
		}
		else{
			String apMacField = "\"apMacAddr\":\"";
			apMacField += apMacAddr;
			apMacField +="\"";
			hostInfo = hostInfo.replace("\"Result\":0", apMacField);
			hostInfo = hostInfo.replace("List", "activeDevices");

			hostInfo = hostInfo.replaceAll("MAC", "deviceMacAddr");
			hostInfo = hostInfo.replaceAll("IP", "deviceIp");

			String url = "http://60.205.212.99/squirrel/v1/devices/ap/devices/active";

			sendPost(url, hostInfo);
		}
	}


}
