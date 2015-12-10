# XCCacheManager
XCCacheManager For Android-Android缓存管理工具库

使用演示图：

![iamge](https://raw.githubusercontent.com/jczmdeveloper/XCCacheManager/master/screenshots/01.gif)

使用方法如下：

public class MainActivity extends Activity implements View.OnClickListener {
    private Button mBtnWrite;
    private Button mBtnRead;
    private EditText mEtText;
    private TextView mTvResult;

    private XCCacheManager mCacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnRead = (Button) findViewById(R.id.btn_read);
        mBtnWrite = (Button) findViewById(R.id.btn_write);
        mEtText = (EditText) findViewById(R.id.et_text);
        mTvResult = (TextView) findViewById(R.id.tv_result);
        mBtnWrite.setOnClickListener(this);
        mBtnRead.setOnClickListener(this);

        mCacheManager = XCCacheManager.getInstance(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_read:
                mTvResult.setText(mCacheManager.readCache("key_demo"));
                break;
            case R.id.btn_write:
                mCacheManager.writeCache("key_demo",mEtText.getText().toString());
                Toast.makeText(this,"write string to cache",Toast.LENGTH_SHORT).show();
                break;
        }
    }
}


