package pref;
import com.nao20010128nao.ToolBox.*;
import android.util.*;
import android.content.*;

public class StartPref extends HandledPreference{
	public static AttributeSet as;
	public StartPref(Context c,AttributeSet attrs){
		super(c,as=attrs);
	}
	public StartPref(Context c){
		super(c,as);
	}
}
