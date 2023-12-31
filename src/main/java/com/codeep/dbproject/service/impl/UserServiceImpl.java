package com.codeep.dbProject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codeep.dbProject.common.ErrorCode;
import com.codeep.dbProject.exception.BusinessException;
import com.codeep.dbProject.mapper.UserMapper;
import com.codeep.dbProject.model.domain.User;
import com.codeep.dbProject.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeep.dbProject.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 24796
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2023-06-12 17:37:24
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，用于混淆密码
     */
    private static final String SALT = "NJFU";

    @Override
    public long userRegister(String userNo, String username, String userPassword, String checkPassword) {
        // 1. 校验
        // 判空
        if (StringUtils.isAnyBlank(userNo, username, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (userNo.length() != 10){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号格式错误");
        }
        if (userPassword.length() < 6 || checkPassword.length() < 6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 学号不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userNo);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号违法");
        }
        // 密码和校验密码一致
        if (userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        // 学号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userNo", userNo);
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号已被注册");
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3. 插入数据
        User user = new User();
        user.setUserNo(userNo);
        user.setUsername(username);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败");
        }
        return user.getId();

    }

    @Override
    public User userLogin(String userNo, String userPassword, HttpServletRequest request) {
        // 1. 校验
        // 判空
        if (StringUtils.isAnyBlank(userNo, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (userNo.length() != 10){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号格式错误");
        }
        // 学号不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userNo);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号违法");
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userNo", userNo);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null){
            return null;
        }

        // 4. 用户脱敏
        User safetyUser = getSafetyUser(user);

        // 5. 往session里记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }


    /**
     * 用户脱敏
     *
     * @param originUser 用户的全部信息
     * @return 用户的脱敏后的信息
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserNo(originUser.getUserNo());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setClassNo(originUser.getClassNo());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }



}




