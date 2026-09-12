package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.app.role.RoleManager;
import android.graphics.Color;
import android.speech.*;
import android.speech.tts.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    LinearLayout content, log;
    TextView status, output;
    boolean lockscreenAssist;
    SpeechRecognizer sr; Intent si; TextToSpeech tts;
    final int BG=Color.rgb(2,6,11), PANEL=Color.rgb(7,15,25), BLUE=Color.rgb(74,163,255), MUTED=Color.rgb(150,165,185);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        lockscreenAssist = getIntent().getBooleanExtra("LOCKSCREEN_ASSIST", false);
        if(lockscreenAssist && Build.VERSION.SDK_INT>=27){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        build(); initTts(); initSpeech();
        if(lockscreenAssist){ new Handler().postDelayed(this::listen, 450); }
    }
    TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(z);v.setPadding(16,10,16,10);return v;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(8,24,42));b.setOnClickListener(v->click(s));return b;}
    void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,14,16,12);root.setBackgroundColor(BG);
        ScrollView sv=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        TextView h=text("JARVIS",30);h.setTextColor(BLUE);h.setTypeface(null,1);content.addView(h);
        TextView sh=text("ДОМАШНИЙ AI-ЦЕНТР",12);sh.setTextColor(MUTED);content.addView(sh);
        content.addView(text("На связи. Что требуется?",25));
        status=text("Система готова",14);status.setTextColor(MUTED);content.addView(status);
        output=text("Голосовая команда или любой инструмент ниже.",16);output.setBackgroundColor(PANEL);content.addView(output,new LinearLayout.LayoutParams(-1,110));
        addSection("⚡ БЫСТРО");
        grid(new String[]{"🎙 Голос","🔐 Экран блокировки","⏱ Таймер","🧮 Расчёт","📝 Заметка"});
        addSection("🏠 ДОМ");
        grid(new String[]{"📐 Ремонт","🎨 Материалы","💡 Электрика","🛒 Покупки"});
        addSection("🍳 КУХНЯ");
        grid(new String[]{"🍳 Готовка","⚖ Порции","🌡 Температура","🥘 Рецепт"});
        addSection("🚗 АВТО");
        grid(new String[]{"⛽ Топливо","🛣 Поездка","🔧 ТО","🛞 Шины"});
        addSection("💰 ФИНАНСЫ");
        grid(new String[]{"💰 Бюджет","🧾 Расход","💱 Валюта","📅 Платежи"});
        addSection("📋 МОИ ДЕЛА");
        grid(new String[]{"✓ Задачи","🔔 Напоминание","📦 Вещи","📷 Камера"});
        addSection("🧠 JARVIS");
        content.addView(text("Можно говорить естественными фразами. JARVIS определяет, какой инструмент нужен, и запускает его.",14));
        sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        Button voice=btn("🎙  ГОВОРИТЬ С JARVIS");root.addView(voice,new LinearLayout.LayoutParams(-1,62));setContentView(root);
    }
    void addSection(String s){TextView v=text(s,13);v.setTextColor(BLUE);v.setPadding(6,22,6,7);content.addView(v);}
    void grid(String[] a){LinearLayout r=null;for(int i=0;i<a.length;i++){if(i%2==0){r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);content.addView(r);}Button b=btn(a[i]);r.addView(b,new LinearLayout.LayoutParams(0,58,1));}}
    void click(String s){
        if(s.contains("Голос")) listen();
        else if(s.contains("Экран блокировки")) enableLockscreenAssistant();
        else if(s.contains("Таймер")) timer();
        else if(s.contains("Расчёт")) calc();
        else if(s.contains("Заметка")) note();
        else if(s.contains("Ремонт")||s.contains("Материал")) repair();
        else if(s.contains("Электрика")) electrical();
        else if(s.contains("Покупки")) shopping();
        else if(s.contains("Готовка")||s.contains("Порции")||s.contains("Температура")||s.contains("Рецепт")) kitchen(s);
        else if(s.contains("Топливо")||s.contains("Поездка")) carCalc();
        else if(s.contains("ТО")||s.contains("Шины")) carInfo(s);
        else if(s.contains("Бюджет")||s.contains("Расход")) finance();
        else if(s.contains("Валюта")) speak("Для актуального курса нужен интернет-источник. В этой версии расчёты без фиктивных курсов.");
        else if(s.contains("Платежи")||s.contains("Напоминание")||s.contains("Задачи")) note();
        else if(s.contains("Камера")) speak("Камера подключается как отдельный модуль распознавания в следующем этапе.");
        else speak("Модуль открыт.");
    }
    void initTts(){tts=new TextToSpeech(this,x->{if(x==0){tts.setLanguage(new Locale("ru","RU"));tts.setPitch(.62f);tts.setSpeechRate(.82f);tts.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANT).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());for(Voice v:tts.getVoices()){String n=v.getName().toLowerCase(Locale.US);if(n.contains("male")||n.contains("man")){tts.setVoice(v);break;}}}});}
    void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this))return;sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){status.setText("JARVIS слушает...");} public void onBeginningOfSpeech(){} public void onRmsChanged(float x){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){status.setText("Анализирую команду...");}
        public void onError(int e){status.setText("Готов");} public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));}
        public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
    });si=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);si.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);si.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");}
    void listen(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);return;}sr.startListening(si);}
    void speak(String s){output.setText(s);if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis");}
    void command(String raw){
        String c=raw.toLowerCase(new Locale("ru"));output.setText("Вы: "+raw);
        if(c.contains("таймер")){int m=num(c);if(m>0){speak("Принято, сэр. Таймер установлен на "+m+" минут.");new Handler().postDelayed(()->speak("Время вышло."),m*60000L);}else timer();}
        else if(c.contains("который час")||c.equals("время"))speak("Сейчас "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
        else if(c.contains("ремонт")||c.contains("краск")||c.contains("комнат"))repair();
        else if(c.contains("купить")||c.contains("покупк"))shopping();
        else if(c.contains("машин")||c.contains("авто")||c.contains("бензин")||c.contains("топлив"))carCalc();
        else if(c.contains("бюджет")||c.contains("расход"))finance();
        else if(c.startsWith("запиши")||c.startsWith("добавь заметку")){save(raw.replaceFirst("(?i)запиши\\s*:??\\s*","").replaceFirst("(?i)добавь заметку\\s*:??\\s*",""));}
        else if(c.contains("привет")||c.contains("джарвис"))speak("На связи, сэр. Говорите.");
        else speak("Команда не распознана. Скажите, например: «посчитай ремонт комнаты», «поставь таймер», «добавь молоко в покупки» или «посчитай расходы на поездку».");
    }

    void enableLockscreenAssistant(){
        if(Build.VERSION.SDK_INT>=29){
            RoleManager rm=(RoleManager)getSystemService(RoleManager.class);
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){
                if(rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    speak("Готово, сэр. JARVIS уже выбран системным ассистентом. Его можно вызвать с заблокированного экрана жестом ассистента.");
                } else {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), 81);
                }
                return;
            }
        }
        speak("На этом устройстве выбор системного ассистента управляется настройками Android.");
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==81){
            boolean ok=Build.VERSION.SDK_INT>=29 && ((RoleManager)getSystemService(RoleManager.class)).isRoleHeld(RoleManager.ROLE_ASSISTANT);
            speak(ok ? "Готово, сэр. Теперь JARVIS можно вызывать с экрана блокировки системным жестом ассистента." : "Выбор ассистента не подтверждён.");
        }
    }

    int num(String s){Matcher m=Pattern.compile("(\\d+)\\s*(мин|минут)").matcher(s);return m.find()?Integer.parseInt(m.group(1)):0;}
    void timer(){EditText e=new EditText(this);e.setHint("Минуты");e.setInputType(2);new AlertDialog.Builder(this).setTitle("⏱ Таймер").setView(e).setPositiveButton("Запустить",(d,w)->{try{int m=Integer.parseInt(e.getText().toString());speak("Таймер на "+m+" минут.");new Handler().postDelayed(()->speak("Время вышло."),m*60000L);}catch(Exception x){}}).setNegativeButton("Отмена",null).show();}
    void calc(){EditText e=new EditText(this);e.setHint("125 * 8");new AlertDialog.Builder(this).setTitle("🧮 Расчёт").setView(e).setPositiveButton("Посчитать",(d,w)->{try{String x=e.getText().toString().replace(" ","").replace(",",".");char[] ops={'+','-','*','/'};int p=-1;char op='+';for(char q:ops){p=x.indexOf(q,1);if(p>0){op=q;break;}}double a=Double.parseDouble(x.substring(0,p)),b=Double.parseDouble(x.substring(p+1));double r=op=='+'?a+b:op=='-'?a-b:op=='*'?a*b:a/b;speak("Расчёт завершён. Результат: "+r);}catch(Exception ex){speak("Не удалось разобрать выражение.");}}).setNegativeButton("Отмена",null).show();}
    void note(){EditText e=new EditText(this);e.setHint("Что сохранить?");new AlertDialog.Builder(this).setTitle("📝 Заметка").setView(e).setPositiveButton("Сохранить",(d,w)->save(e.getText().toString())).setNegativeButton("Отмена",null).show();}
    void save(String s){if(s==null||s.trim().isEmpty()){speak("Пустую заметку не сохраняю.");return;}getPreferences(0).edit().putString("note",s.trim()).apply();speak("Готово, сэр. Записал.");}
    void repair(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText a=new EditText(this);a.setHint("Длина комнаты, м");EditText b=new EditText(this);b.setHint("Ширина, м");EditText h=new EditText(this);h.setHint("Высота, м");EditText o=new EditText(this);o.setHint("Площадь окон + дверей, м²");l.addView(a);l.addView(b);l.addView(h);l.addView(o);new AlertDialog.Builder(this).setTitle("📐 Расчёт ремонта").setView(l).setPositiveButton("Рассчитать",(d,w)->{try{double area=2*(Double.parseDouble(a.getText().toString())+Double.parseDouble(b.getText().toString()))*Double.parseDouble(h.getText().toString())-Double.parseDouble(o.getText().toString());speak("Площадь стен: "+String.format(Locale.US,"%.2f",area)+" квадратных метров.");}catch(Exception x){speak("Заполните размеры.");}}).setNegativeButton("Отмена",null).show();}
    void electrical(){speak("Электрика: могу рассчитать мощность, ток и нагрузку. Используйте калькулятор или скажите: «посчитай 220 вольт и 1500 ватт»." );}
    void shopping(){note();}
    void kitchen(String s){speak(s.contains("Температура")?"Укажите блюдо и способ приготовления, чтобы подобрать температуру.":s.contains("Порции")?"Укажите исходное количество порций и нужное количество.":"Скажите, что готовим, и какие продукты есть дома.");}
    void carCalc(){EditText l=new EditText(this);l.setHint("Километры");EditText fuel=new EditText(this);fuel.setHint("Расход л/100 км");EditText price=new EditText(this);price.setHint("Цена за литр");LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.addView(l);x.addView(fuel);x.addView(price);new AlertDialog.Builder(this).setTitle("⛽ Поездка").setView(x).setPositiveButton("Рассчитать",(d,w)->{try{double km=Double.parseDouble(l.getText().toString()),f=Double.parseDouble(fuel.getText().toString()),p=Double.parseDouble(price.getText().toString());double liters=km*f/100; speak(String.format(Locale.US,"Расход %.2f литра. Стоимость %.2f.",liters,liters*p));}catch(Exception e){speak("Проверьте данные.");}}).setNegativeButton("Отмена",null).show();}
    void carInfo(String s){speak(s.contains("Шины")?"Проверяйте давление по данным производителя автомобиля.":"В журнале ТО можно хранить дату, пробег и следующую замену.");}
    void finance(){note();}
    @Override protected void onDestroy(){if(sr!=null)sr.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}