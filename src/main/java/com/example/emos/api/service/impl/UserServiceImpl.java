package com.example.emos.api.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.db.dao.TbUserDao;
import com.example.emos.api.db.pojo.TbUser;
import com.example.emos.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Feather
 */
@Service
public class UserServiceImpl implements UserService {
    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

//    @Value("${workflow.url}")
//    private String workflow;

//    @Value("${emos.code}")
//    private String code;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TbUserDao userDao;

    @Override
    public HashMap createQrCode() {
        String uuid = IdUtil.simpleUUID();
        redisTemplate.opsForValue().set(uuid, false, 5, TimeUnit.MINUTES);
        QrConfig config = new QrConfig();
        config.setHeight(160);
        config.setWidth(160);
        config.setMargin(1);
        String base64 = QrCodeUtil.generateAsBase64("login@@@" + uuid, config, ImgUtil.IMAGE_TYPE_JPG);
        HashMap map = new HashMap() {{
            put("uuid", uuid);
            put("pic", base64);
        }};
        return map;
    }

    @Override
    public boolean checkQrCode(String code, String uuid) {
        boolean bool = redisTemplate.hasKey(uuid);
        if (bool) {
            String openId = getOpenId(code);
            long userId = userDao.searchIdByOpenId(openId);
            redisTemplate.opsForValue().set(uuid, userId);
        }
        return bool;
    }

    @Override
    public HashMap wechatLogin(String uuid) {
        HashMap map = new HashMap();
        boolean result = false;
        if (redisTemplate.hasKey(uuid)) {
            String value = redisTemplate.opsForValue().get(uuid).toString();
            if (!"false".equals(value)) {
                result = true;
                redisTemplate.delete(uuid);
                int userId = Integer.parseInt(value);
                map.put("userId", userId);
            }
        }
        map.put("result", result);
        return map;
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions = userDao.searchUserPermissions(userId);
        return permissions;
    }

    @Override
    public HashMap searchById(int userId) {
        HashMap map = userDao.searchById(userId);
        return map;
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userDao.searchUserSummary(userId);
        return map;
    }

    @Override
    public ArrayList<HashMap> searchAllUser() {
        ArrayList<HashMap> list = userDao.searchAllUser();
        return list;
    }

    /**
     * 登录
     *
     * @param param
     * @return
     */
    @Override
    public Integer login(HashMap param) {
        return userDao.login(param);
    }

    //个人信息修改
    @Override
    public int updateUserInfo(HashMap param) {
        int rows = userDao.updateUserInfo(param);
        return rows;
    }

    //密码修改
    @Override
    public int updatePassword(HashMap param) {
        int rows = userDao.updatePassword(param);
        return rows;
    }

    //原密码校验
    @Override
    public Boolean passwordVerify(HashMap param) {
        return userDao.passwordVerify(param) != null;
    }

    @Override
    public PageUtils searchUserByPage(HashMap param) {
        ArrayList<HashMap> list = userDao.searchUserByPage(param);
        long count = userDao.searchUserCount(param);
        int start = (Integer) param.get("start");
        int pageSize = (Integer) param.get("pageSize");
        return new PageUtils(list, count, start, pageSize);
    }

    @Override
    public int insert(TbUser tbUser) {
        return userDao.insert(tbUser);
    }

    @Override
    public int update(HashMap param) {
        return userDao.update(param);
    }

    @Override
    public int deleteUserByIds(Integer[] ids) {
        return userDao.deleteUserByIds(ids);
    }

    private String getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    @Override
    public ArrayList<String> searchUserRoles(int userId) {
        ArrayList<String> list = userDao.searchUserRoles(userId);
        return list;
    }


    @Override
    public HashMap searchNameAndDept(int userId) {
        HashMap map = userDao.searchNameAndDept(userId);
        return map;
    }


}
