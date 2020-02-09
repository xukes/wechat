package com.bc.wechat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.bc.wechat.R;
import com.bc.wechat.adapter.AreaAdapter;
import com.bc.wechat.dao.AreaDao;
import com.bc.wechat.entity.Area;

import java.util.List;

/**
 * 省份选择
 *
 * @author zhou
 */
public class PickProvinceActivity extends FragmentActivity {
    private ListView mProvinceLv;
    private AreaAdapter mProvinceAdapter;

    private AreaDao mAreaDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_province_picker);
        initView();
        mAreaDao = new AreaDao();

        final List<Area> areaList = mAreaDao.getProvinceList();
        mProvinceAdapter = new AreaAdapter(this, areaList);
        mProvinceLv.setAdapter(mProvinceAdapter);
        mProvinceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Area area = areaList.get(position);
                Intent intent = new Intent(PickProvinceActivity.this, PickCityActivity.class);
                intent.putExtra("provinceName", area.getName());
                startActivity(intent);
            }
        });
    }

    public void back(View view) {
        finish();
    }

    private void initView() {
        mProvinceLv = findViewById(R.id.lv_area);
    }
}
