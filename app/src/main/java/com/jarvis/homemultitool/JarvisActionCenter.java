package com.jarvis.homemultitool;

import android.content.*;
import org.json.*;
import java.util.*;

/** Small persistent action ledger for ACTIVE/SCHEDULED/COMPLETED/FAILED/CANCELLED tasks. */
public final class JarvisActionCenter {
    public static final String ACTIVE="ACTIVE", SCHEDULED="SCHEDULED", COMPLETED="COMPLETED", FAILED="FAILED", CANCELLED="CANCELLED";
    private static final String PREF="jarvis_action_center", KEY="items"; private static final int MAX=40;
    private final SharedPreferences sp;
    public JarvisActionCenter(Context c){sp=c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public synchronized String start(String title,List<String> steps){
        String id=UUID.randomUUID().toString(); JSONObject o=new JSONObject();try{o.put("id",id);o.put("title",title);o.put("status",ACTIVE);o.put("created",System.currentTimeMillis());o.put("steps",new JSONArray(steps));o.put("completed",0);}catch(Exception ignored){} JSONArray a=read();a.put(o);trim(a);write(a);return id;
    }
    public synchronized void progress(String id,int completed){update(id,ACTIVE,completed);}
    public synchronized void complete(String id){update(id,COMPLETED,-1);}
    public synchronized void fail(String id){update(id,FAILED,-1);}
    public synchronized void cancel(String id){update(id,CANCELLED,-1);}
    public synchronized JSONArray snapshot(){return read();}
    public synchronized String summary(){JSONArray a=read();int active=0,scheduled=0,done=0,failed=0;for(int i=0;i<a.length();i++){String s=a.optJSONObject(i).optString("status");if(ACTIVE.equals(s))active++;else if(SCHEDULED.equals(s))scheduled++;else if(COMPLETED.equals(s))done++;else if(FAILED.equals(s))failed++;}return "Активные: "+active+"\nЗапланированные: "+scheduled+"\nЗавершённые: "+done+"\nОшибки: "+failed;}
    public synchronized String formatted(){JSONArray a=read();if(a.length()==0)return "Центр задач пуст.";StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);b.append("• ").append(o.optString("title")).append(" — ").append(label(o.optString("status"))).append('\n');}return b.toString().trim();}
    private String label(String s){if(ACTIVE.equals(s))return "выполняется";if(SCHEDULED.equals(s))return "запланирована";if(COMPLETED.equals(s))return "завершена";if(FAILED.equals(s))return "ошибка";return "отменена";}
    private void update(String id,String status,int completed){JSONArray a=read();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(id.equals(o.optString("id"))){try{o.put("status",status);if(completed>=0)o.put("completed",completed);}catch(Exception ignored){}break;}}write(a);}
    private JSONArray read(){try{return new JSONArray(sp.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
    private void write(JSONArray a){sp.edit().putString(KEY,a.toString()).apply();}
    private void trim(JSONArray a){while(a.length()>MAX)a.remove(0);}
}
