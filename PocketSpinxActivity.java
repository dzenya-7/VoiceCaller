public class PocketSphinxActivity extends Activity implements

RecognitionListener {

String[] names;

private TextToSpeech mTextToSpeech;

private final Queue<String> mSpeechQueue = new LinkedList<String>();

ArrayList<Contact> contacts = new ArrayList<>();

static String KWS_SEARCH = "вставай";

static String MENU_SEARCH = "меню";

static String NUMBER_SEARCH = "номер";

static String RECALL_SEARCH = "перезвони";

static String CONTACT_SEARCH = "контакт";

static String CANCEL = "отмена";

static String KEYPHRASE = "бела";

String[]commands={"ноль","один","два","три","четыре","пять","шесть","семь","восемь","девять","ок",CONTACT_SEARCH,CANCEL,KEYPHRASE,RECALL_SEARCH,NUMBER_SEARCH};

private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

ArrayList<Call> calls_1 = new ArrayList<>();

private SpeechRecognizer recognizer;

private HashMap<String, Integer> captions;

private Context activity;

private boolean hasContactPermissin() {

return

ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

&& ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

&& ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

&& ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

&& ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

}

@RequiresApi(api = Build.VERSION_CODES.M)

public void onRequestPermRes(int requestCode, @NonNull String[] permission, @NonNull int[] grantRes) {

if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {

if (grantRes[0] != PackageManager.PERMISSION_GRANTED) {

makeText(this, " Дай разрешение!", Toast.LENGTH_LONG).show();

}

}

super.onRequestPermissionsResult(requestCode, permission, grantRes);

}

private void requestContactPermission() {

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

requestPermissions(new String[]{

Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO

}, PERMISSIONS_REQUEST_RECORD_AUDIO);

}

}

@Override

public void onCreate(Bundle state) {

super.onCreate(state);

setContentView(R.layout.main);

try {

if (hasContactPermissin()) {

try {

ContentResolver resolver = getContentResolver();

Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{

ContactsContract.CommonDataKinds.Phone._ID,

ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,

ContactsContract.CommonDataKinds.Phone.NUMBER

}, null, null, null);

if (cursor != null && cursor.moveToFirst()) {

do {

long id = cursor.getLong(0);

String name = cursor.getString(1);

String number = cursor.getString(2);

Contact contact = new Contact(id, name, number);

contacts.add(contact);

} while (cursor.moveToNext());

}

if (cursor != null) {

cursor.close();

}

ContentResolver resolver_1 = getContentResolver();

@SuppressLint("MissingPermission") Cursor cursor_1 = resolver_1.query(CallLog.Calls.CONTENT_URI, new String[]{

CallLog.Calls._ID,

CallLog.Calls.DATE,

CallLog.Calls.NUMBER

}, null, null, null);

if (cursor_1 != null && cursor_1.moveToFirst()) {

do {

long id = cursor_1.getLong(0);

String number = cursor_1.getString(2);

@SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

format.setTimeZone(TimeZone.getTimeZone("GMT"));

String name = format.format(new Date(Long.parseLong(cursor_1.getString(1))));

Call call = new Call(id, number, name);

calls_1.add(call);

} while (cursor_1.moveToNext());

}

if (cursor_1 != null) {

cursor_1.close();

}

ListView rv_1 = findViewById(R.id.rv_1);

rv_1.setAdapter(new CallAdapter(this, R.layout.contact_view_1, calls_1));

AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

@SuppressLint("MissingPermission")

@Override

public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

Call selectedCall = (Call) parent.getItemAtPosition(position);

String toDial = "tel:" + selectedCall.getNumber();

startActivity(new Intent(ACTION_CALL, Uri.parse(toDial)));

}

};

rv_1.setOnItemClickListener(onItemClickListener);

ListView countriesList = findViewById(R.id.rv);

countriesList.setAdapter(new ContactAdapter(this, R.layout.contact_view_1, contacts));

AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {

@SuppressLint("MissingPermission")

@Override

public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

Contact selectedState = (Contact) parent.getItemAtPosition(position);

String toDial = "tel:" + selectedState.getNumber();

startActivity(new Intent(ACTION_CALL, Uri.parse(toDial)));

}

};

countriesList.setOnItemClickListener(itemListener);

captions = new HashMap<>();

captions.put(KWS_SEARCH, R.string.kws_caption);

captions.put(MENU_SEARCH, R.string.menu_caption);

captions.put(CONTACT_SEARCH, R.string.contact_caption);

captions.put(NUMBER_SEARCH, R.string.number_caption);

captions.put(RECALL_SEARCH, R.string.recall_caption);

TextView textView = findViewById(R.id.caption_text);

textView.setText("Preparing recognizer");

new SetupTask(this).execute();

} catch (Exception e) {

Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

}

} else {

Toast.makeText(this, "Предоставьте разрешение!", Toast.LENGTH_LONG).show();

requestContactPermission();

}

} catch (Exception e) {

Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

}

}

}).create();

builder.show();

}

public Context getActivity() {

return activity;

}

private static class SetupTask extends AsyncTask<Void, Void, Exception> {

WeakReference<PocketSphinxActivity> activityReference;

SetupTask(PocketSphinxActivity activity) {

this.activityReference = new WeakReference<>(activity);

}

@Override

protected Exception doInBackground(Void... params) {

try {

Assets assets = new Assets(activityReference.get());

File assetDir = assets.syncAssets();

activityReference.get().setupRecognizer(assetDir);

} catch (Exception e) {

return e;

}

return null;

}

@Override

protected void onPostExecute(Exception result) {

if (result != null) {

((TextView) activityReference.get().findViewById(R.id.caption_text))

.setText("Failed to init recognizer " + result);

} else {

activityReference.get().switchSearch(KWS_SEARCH);

}

}

}

@Override

public void onRequestPermissionsResult(int requestCode,

@NonNull String[] permissions, @NonNull int[] grantResults) {

super.onRequestPermissionsResult(requestCode, permissions, grantResults);

if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {

if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

new SetupTask(this).execute();

} else {

finish();

}

}

}

@Override

public void onDestroy() {

super.onDestroy();

if (recognizer != null) {

recognizer.cancel();

recognizer.shutdown();

}

}

@Override

public void onPartialResult(Hypothesis hypothesis) {

if (hypothesis == null)

return;

String text = hypothesis.getHypstr();

if (text.equals(KEYPHRASE))

switchSearch(MENU_SEARCH);

else if (text.equals(NUMBER_SEARCH))

switchSearch(NUMBER_SEARCH);

else if (text.equals(CONTACT_SEARCH))

switchSearch(CONTACT_SEARCH);

else if (text.equals(RECALL_SEARCH))

switchSearch(RECALL_SEARCH);

else{

for(int i=0;i<contacts.size();i++){

if(text.equals(contacts.get(i).getName().toLowerCase())){

Contact selectedState = (contacts.get(i));

String toDial = "tel:" + selectedState.getNumber();

startActivity(new Intent(ACTION_CALL, Uri.parse(toDial)));

}

}

if(text.equals("ноль"))text = "0";

if(text.equals("один"))text = "1";

if(text.equals("два"))text = "2";

if(text.equals("три"))text = "3";

if(text.equals("четыре"))text = "4";

if(text.equals("пять"))text = "5";

if(text.equals("шесть"))text = "6";

if(text.equals("семь"))text = "7";

if(text.equals("восемь"))text = "8";

if(text.equals("девять"))text = "9";

((TextView) findViewById(R.id.result_text)).setText(text);

}

}

@Override

public void onResult(Hypothesis hypothesis) {

((TextView) findViewById(R.id.result_text)).setText("");

if (hypothesis != null) {

String text = hypothesis.getHypstr();

makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

}

}

@Override

public void onBeginningOfSpeech() {

}

@Override

public void onEndOfSpeech() {

if (!recognizer.getSearchName().equals(KWS_SEARCH))

switchSearch(KWS_SEARCH);

}

@SuppressLint("MissingPermission")

private void switchSearch(String searchName) {

recognizer.stop();

if (searchName.equals(KWS_SEARCH))

recognizer.startListening(searchName);

else if (searchName.equals(RECALL_SEARCH)) {

Call selectedCall = calls_1.get(calls_1.size() - 1);

String toDial = "tel:" + selectedCall.getNumber();

startActivity(new Intent(ACTION_CALL, Uri.parse(toDial)));

}

else

recognizer.startListening(searchName, 100000);

String caption = getResources().getString(captions.get(searchName));

((TextView) findViewById(R.id.caption_text)).setText(caption);

}

private void setupRecognizer(File assetsDir) throws Exception {

File dictionary= new File(assetsDir, "cmudict-en-us.dict");

File contactGrammar = new File(assetsDir, "contact.gram");

names = new String[contacts.size()];

for (int i = 0; i < contacts.size(); i++) {

names[i] = contacts.get(i).getName().toLowerCase();

}

Grammar grammar= new Grammar(names);

for(int i = 0; i<commands.length; i++){

grammar.addWords(commands[i]);

}

String gram= grammar.getJsgf();

String dict=grammar.getDict();

FileUtils.write(contactGrammar,gram);

FileUtils.write(dictionary,dict);

recognizer = SpeechRecognizerSetup.defaultSetup()

.setAcousticModel(new File(assetsDir, "en-us-ptm"))

.setDictionary(dictionary)

.setRawLogDir(assetsDir)

.getRecognizer();

recognizer.addListener(this);

recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

recognizer.addGrammarSearch(CONTACT_SEARCH, contactGrammar);

recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

File menuGrammar = new File(assetsDir, "menu.gram");

recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

File recallGrammar = new File(assetsDir, "

recognizer.addGrammarSearch(RECALL_SEARCH, recallGrammar);

File numberGrammar = new File(assetsDir, "number.gram");

recognizer.addGrammarSearch(NUMBER_SEARCH, numberGrammar);

}

@Override

public void onError(Exception error) {

((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());

}

@Override

public void onTimeout() {

switchSearch(KWS_SEARCH);

}

}