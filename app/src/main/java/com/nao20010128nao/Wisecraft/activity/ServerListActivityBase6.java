package com.nao20010128nao.Wisecraft.activity;
import android.view.*;
import com.nao20010128nao.Wisecraft.misc.*;
import java.util.*;

//ContextMenu
class ServerListActivityBase6 extends ServerListActivityBaseFields {
	protected Map<View,Duo<Treatment<Duo<View,ContextMenu>>,Predicate<Trio<View,ContextMenu,MenuItem>>>> contextMenuHandlers=new HashMap<>();
	protected Map<Menu,View> contextMenuObjects=new HashMap<>();
	protected Map<MenuItem,View> contextMenuItems=new HashMap<>();
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		contextMenuObjects.put(menu,v);
		if(contextMenuHandlers.containsKey(v)){
			Treatment<Duo<View,ContextMenu>> init=contextMenuHandlers.get(v).getA();
			if(init!=null){
				init.process(new Duo<View,ContextMenu>(v,menu));
				for(int i=0;i<menu.size();i++){
					contextMenuItems.put(menu.getItem(i),v);
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(contextMenuItems.containsKey(item)){
			View owner=contextMenuItems.get(item);
			if(contextMenuHandlers.containsKey(owner)){//it must be true
				ContextMenu menu=null;
				for(Map.Entry<Menu,View> value:contextMenuObjects.entrySet()){
					if(value.getValue()==owner&&value.getValue() instanceof ContextMenu){
						menu=(ContextMenu)value.getKey();
					}
				}
				Predicate<Trio<View,ContextMenu,MenuItem>> selection=contextMenuHandlers.get(owner).getB();
				if(selection!=null){
					return selection.process(new Trio<View,ContextMenu,MenuItem>(owner,menu,item));
				}
			}
		}
		
		return super.onContextItemSelected(item);
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		super.onContextMenuClosed(menu);
		for(int i=0;i<menu.size();i++){
			contextMenuItems.remove(menu.getItem(i));
		}
		contextMenuHandlers.remove(contextMenuObjects.get(menu));
		contextMenuObjects.remove(menu);
	}

	public void openContextMenu(View view,Treatment<Duo<View,ContextMenu>> init,Predicate<Trio<View,ContextMenu,MenuItem>> selection) {
		contextMenuHandlers.put(view,new Duo<Treatment<Duo<View,ContextMenu>>,Predicate<Trio<View,ContextMenu,MenuItem>>>(init,selection));
		if(!view.showContextMenu())
			contextMenuHandlers.remove(view);
	}
}
