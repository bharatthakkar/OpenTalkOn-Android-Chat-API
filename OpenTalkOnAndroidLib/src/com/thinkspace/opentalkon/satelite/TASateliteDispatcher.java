package com.thinkspace.opentalkon.satelite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.thinkspace.opentalkon.OTOApp;
import com.thinkspace.opentalkon.data.RegsiterData;
import com.thinkspace.opentalkon.data.TANotify;
import com.thinkspace.opentalkon.data.TAUserInfo;
import com.thinkspace.opentalkon.data.TAUserNick;

public class TASateliteDispatcher{
	public final static String LOCATION_KEY = "location";
	public final static String STATE_KEY = "state";
	public final static String DATA_KEY = "data";
	
	public final static String OK_MSG = "ok";
	
	public static class DispatchedData{
		public Map<String, String> stateMap = new HashMap<String, String>();
		public Map<String, String> errMap = new HashMap<String, String>();
		public Map<String, Object> dataMap = new HashMap<String, Object>();
		
		public boolean hasLocation(String location){
			return stateMap.containsKey(location);
		}
		
		String set_location;
		public void setLocation(String location){
			this.set_location = location;
		}
		public String getLocation(){
			return set_location;
		}
		public boolean isOK(){
			return isOK(set_location);
		}
		public String getState(){
			return getState(set_location);
		}
		public Object getData(){
			return getData(set_location);
		}
		public String getErrMsg(){
			return getErrMsg(set_location);
		}
		
		public boolean isOK(String location){
			String state = getState(location);
			if(state == null) return false;
			if(state.equals(OK_MSG)) return true;
			return false;
		}
		public String getState(String location){
			try{ 
				return stateMap.get(location);
			}catch(Exception ex){ }
			return null;
		}
		public Object getData(String location){
			try{ 
				return dataMap.get(location);
			}catch(Exception ex){ }
			return null;
		}
		public String getErrMsg(String location){
			try{ 
				return errMap.get(location);
			}catch(Exception ex){}
			return null;
		}
	}
	public static DispatchedData dispatchSateliteData(JSONObject data){
		DispatchedData retData = new DispatchedData();
		
		try{
			String location = data.getString(LOCATION_KEY);
			String state = data.getString(STATE_KEY);
			
			retData.setLocation(location);
			retData.stateMap.put(location, state);
			if(state.equals(OK_MSG)){
				if(TASatelite.REGISTER_URL.endsWith(location)){
					JSONObject rData = data.getJSONObject(DATA_KEY);
					RegsiterData regData = new RegsiterData(rData);
					retData.dataMap.put(location, regData);
				}else if(TASatelite.GET_FRIENDS_URL.endsWith(location) ||
					TASatelite.GET_USER_INFOS_URL.endsWith(location) ||
					TASatelite.GET_USER_INFO_BY_NICK_URL.endsWith(location) ||
					TASatelite.GET_REVERSE_FRIENDS_URL.endsWith(location)){
					JSONArray arrData = data.getJSONArray(DATA_KEY);
					ArrayList<TAUserInfo> userInfo = new ArrayList<TAUserInfo>();
					if(OTOApp.getInstance().getDB().beginTransaction()){
						for(int i=0;i<arrData.length();++i){
							JSONObject userData = arrData.getJSONObject(i);
							TAUserInfo userInfoElem = dispatchUserInfo(userData, true);
							userInfo.add(userInfoElem);
						}
						OTOApp.getInstance().getDB().endTransaction();
					}
					retData.dataMap.put(location, userInfo);
				}else if(TASatelite.GET_USER_INFO_URL.endsWith(location) ||
						TASatelite.SET_USER_INFO_URL.endsWith(location)){
					JSONObject rData = data.getJSONObject(DATA_KEY);
					OTOApp.getInstance().getDB().beginTransaction();
					retData.dataMap.put(location, dispatchUserInfo(rData, true));
					OTOApp.getInstance().getDB().endTransaction();
				}else if(TASatelite.GET_UNRECEIVED_MSG_URL.endsWith(location)){
					JSONArray arrData = data.getJSONArray(DATA_KEY);
					for(int i=0;i<arrData.length();++i){
						JSONObject msgData = arrData.getJSONObject(i);
						dispatchMsg(msgData);
					}
				}else{
					retData.dataMap.put(location, data);
				}
			}else{
				retData.errMap.put(location, state);
			}
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
		
		return retData;
	}
	
	public static void dispatchMsg(JSONObject data) throws JSONException{
		OTOApp.getInstance().getPushClient().parseTalkMsg(data);
	}
	
	public static TANotify dispatchNotify(JSONObject data) throws JSONException{
		JSONObjectNonException nData = new JSONObjectNonException(data);
		
		int type = data.getInt("type");
		TANotify notify = new TANotify(type);
		notify.setPost_id(nData.getLong("post_id"));
		notify.setUser_id(nData.getString("user_id"));
		notify.setMsg(nData.getString("msg"));
		notify.setTitle(nData.getString("title"));
		notify.setTag(nData.getString("tag"));
		return notify;
	}
	
	public static TAUserInfo dispatchUserInfo(JSONObject data, boolean saveNickWithTransact) throws JSONException{
		TAUserInfo user = new TAUserInfo();
		user.setId(data.getLong("id"));
		user.setNickName(data.getString("nick_name"));
		user.setIntroduce(data.getString("introduce"));
		
		user.setLocale(data.getString("locale"));
		user.setImagePath(data.getString("image_path"));
		
		if(data.has("friend_best")){
			user.setFriend_best(data.getBoolean("friend_best"));
		}else{
			user.setFriend_best(false);
		}
		
		if(data.has("friend")){
			user.set_friend(data.getBoolean("friend"));
		}else{
			user.set_friend(false);
		}
		
		if(data.has("facebook_id")){
			user.setFacebook_id(data.getString("facebook_id"));
		}else{
			user.setFacebook_id(null);
		}

		if(data.has("deny_invitation")){
			user.setDeny_invitation(data.getBoolean("deny_invitation"));
		}else{
			user.setDeny_invitation(false);
		}
		
		if(data.has("invite_from_friend")){
			user.setInvite_from_friend(data.getBoolean("invite_from_friend"));
		}else{
			user.setInvite_from_friend(false);
		}
		
		if(data.has("agree_term")){
			user.agree_term = data.getBoolean("agree_term");
		}else{
			user.agree_term = true;
		}
		
		if(data.has("level")){
			user.level = data.getInt("level");
		}else{
			user.level = 1;
		}
		
		String friendNick = null;
		if(data.has("friend_name")){
			friendNick = data.getString("friend_name");
		}
		
		if(saveNickWithTransact){
			TAUserNick.getInstance().insertWithBeginTransaction(user.getId(), user.getNickName(), friendNick);
		}else{
			if(OTOApp.getInstance().getDB().beginTransaction()){
				TAUserNick.getInstance().insertWithBeginTransaction(user.getId(), user.getNickName(), friendNick);
				OTOApp.getInstance().getDB().endTransaction();
			}
		}
		
		String nickName = TAUserNick.getInstance().getUserInfo(user.getId());
		if(nickName.equals(user.getNickName()) == false){
			user.setNickName(nickName);
		}
		
		return user;
	}
	
	public static class JSONObjectNonException{
		JSONObject data;
		public JSONObjectNonException(JSONObject data) {
			this.data = data;
		}
		
		public Object get(String name) throws JSONException {
			try{
				return data.get(name);
			}catch(JSONException ex){
				return null;
			}
		}

		public boolean getBoolean(String name) throws JSONException {
			try{
				return data.getBoolean(name);
			}catch(JSONException ex){
				return false;
			}
		}

		public double getDouble(String name) throws JSONException {
			try{
				return data.getDouble(name);
			}catch(JSONException ex){
				return 0.0;
			}
		}

		public int getInt(String name) throws JSONException {
			try{
				return data.getInt(name);
			}catch(JSONException ex){
				return 0;
			}
		}

		public JSONArray getJSONArray(String name) throws JSONException {
			try{
				return data.getJSONArray(name);
			}catch(JSONException ex){
				return null;
			}
		}

		public JSONObject getJSONObject(String name) throws JSONException {
			try{
				return data.getJSONObject(name);
			}catch(JSONException ex){
				return null;
			}
		}

		public long getLong(String name) throws JSONException {
			try{
				return data.getLong(name);
			}catch(JSONException ex){
				return 0L;
			}
		}

		public String getString(String name) throws JSONException {
			try{
				return data.getString(name);
			}catch(JSONException ex){
				return null;
			}
		}
		
	}
}
