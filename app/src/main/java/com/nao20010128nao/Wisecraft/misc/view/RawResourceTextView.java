package com.nao20010128nao.Wisecraft.misc.view;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import com.nao20010128nao.Wisecraft.R;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.IOException;

public class RawResourceTextView extends AppCompatTextView
{
	public RawResourceTextView(android.content.Context context) {
		super(context);
	}

    public RawResourceTextView(android.content.Context context, AttributeSet attrs) {
		super(context,attrs);
		loadAttrs(context,attrs);
	}

    public RawResourceTextView(android.content.Context context, AttributeSet attrs, int defStyleAttr) {
		super(context,attrs,defStyleAttr);
		loadAttrs(context,attrs);
	}
	
	private void loadAttrs(Context context,AttributeSet attrs){
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RawResourceTextView);
		String rawRes = array.getString(R.styleable.RawResourceTextView_rawRes);
		int rawResId;
		try {
			rawResId = R.raw.class.getField(rawRes).get(null);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		setText(readAllData(context.getResources().openRawResource(rawResId)));
		array.recycle();
	}
	private String readAllData(InputStream is){
		InputStreamReader isr=null;
		StringWriter sw=new StringWriter();
		char[] buf=new char[4096];
		try {
			isr = new InputStreamReader(is);
			while (true) {
				int r=isr.read(buf);
				if (r <= 0) {
					return sw.toString();
				}
				sw.write(buf, 0, r);
			}
		} catch (IOException e) {
			return "";
		}
	}
}
