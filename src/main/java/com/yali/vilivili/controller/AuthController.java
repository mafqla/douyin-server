package com.yali.vilivili.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yali.vilivili.controller.base.BaseController;
import com.yali.vilivili.controller.base.OR;
import com.yali.vilivili.mapper.CollectionMapper;
import com.yali.vilivili.mapper.VideosEntityMapper;
import com.yali.vilivili.model.entity.CollectionEntity;
import com.yali.vilivili.model.entity.LikeEntity;
import com.yali.vilivili.model.entity.UserEntity;
import com.yali.vilivili.model.entity.VideosEntity;
import com.yali.vilivili.model.ro.AddUserRO;
import com.yali.vilivili.model.ro.EmailRO;
import com.yali.vilivili.model.ro.LoginRO;
import com.yali.vilivili.model.ro.RegisterRO;
import com.yali.vilivili.model.vo.LoginVO;
import com.yali.vilivili.repository.LikeRepository;
import com.yali.vilivili.service.AuthService;
import com.yali.vilivili.utils.HostHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @Description 登录注册接口
 * @Date 2022/11/16 22:45
 * @Author pq
 */
@RestController
@RequestMapping("/auth")
@Api(value = "账号相关", tags = {"账户相关"})
public class AuthController extends BaseController {
    @Resource
    private AuthService authService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private VideosEntityMapper videosEntityMapper;

    @Resource
    private HostHolder hostHolder;

    @Resource
    private CollectionMapper collectionMapper;

    @ApiOperation(value = "登录")
    @PostMapping("/login")
    public ResponseEntity<OR<LoginVO>> login(@Validated LoginRO ro, BindingResult br){
        return this.processData(()->authService.login(ro),br,"登录成功",this::processException);
    }

    @ApiOperation(value = "退出登录")
    @PostMapping("/logout")
    public ResponseEntity<OR<Void>> logout(HttpServletRequest request){
        String token = request.getHeader("loginToken");
        authService.logout(token);
        return process(this::successResult);
    }

    @ApiOperation(value = "发送验证码")
    @PostMapping("/sendCode")
    public ResponseEntity<OR<Void>> sendCode(@Valid EmailRO ro){
        authService.sendEmailCode(ro);
        return process(this::successResult);
    }

    @ApiOperation(value = "注册")
    @PostMapping("/register")
    public ResponseEntity<OR<Void>> register(@Valid RegisterRO ro){
        authService.register(ro);
        return process(this::successResult);
    }

    @ApiOperation(value = "重置密码")
    @PostMapping("/resetPassword")
    public ResponseEntity<OR<Void>> resetPassword(String email,  String password, String code){
        authService.resetPassword(email,password,code);
        return process(this::successResult);
    }

    @ApiOperation(value = "修改用户头像")
    @PostMapping("/updateAvatar")
    public ResponseEntity<OR<Void>> updateAvatar(String email, String avatar){
        authService.updateAvatar(email,avatar);
        return process(this::successResult);
    }


    @ApiOperation(value = "验证码登录")
    @PostMapping("/emailLogin")
    public ResponseEntity<OR<AddUserRO>> emailLogin(String email, String code){
        AddUserRO addUserRO = authService.emailLogin(email, code);
        return processData(()->addUserRO,this::processException);

    }


    @ApiOperation(value = "收藏视频")
    @PostMapping("/collect")
    public ResponseEntity<OR<Void>> emailLogin (String ffid){
        UserEntity userEntity = hostHolder.get();
        CollectionEntity entity = new CollectionEntity();
        entity.setUserId(userEntity.getId());
        entity.setVideoId(Long.parseLong(ffid));
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        collectionMapper.insert(entity);

        LambdaQueryWrapper<VideosEntity> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(VideosEntity::getId,Long.parseLong(ffid));
        VideosEntity videosEntity = videosEntityMapper.selectOne(wrapper);

        redisTemplate.opsForSet().add(userEntity.getUsername()+"收藏",videosEntity);

        return process(this::successResult);
    }

    @ApiOperation(value = "查询收藏")
    @PostMapping("/selectCollection")
    public ResponseEntity<OR<List<VideosEntity>>> emailLogin (){
        UserEntity userEntity = hostHolder.get();

        Set<Object> members = redisTemplate.opsForSet().members(userEntity.getUsername() + "收藏");
        ArrayList<VideosEntity> list = new ArrayList<>();

        for (Object member : members) {
            list.add((VideosEntity) member);
        }

       return processData(()->list,"操作成功",this::processException);
    }

    @Autowired
    LikeRepository likeRepository;

    @ApiOperation(value = "喜欢视频")
    @PostMapping("/like")
    public ResponseEntity<OR<Void>> like (Long vid) {
        VideosEntity videosEntity = videosEntityMapper.selectById(vid);
        LikeEntity like = new LikeEntity();
        like.setLikeTime(new Date());
        UserEntity user = hostHolder.get();
        like.setUserId(user.getId());
        like.setVideoId(videosEntity.getId());
        likeRepository.save(like);

        redisTemplate.opsForSet().add(user.getUsername()+"喜欢",videosEntity);

        return process(this::successResult);
    }

    @ApiOperation(value = "查询喜欢")
    @PostMapping("/selectLike")
    public ResponseEntity<OR<List<LikeEntity>>> mylike (){
        UserEntity userEntity = hostHolder.get();

        Set<Object> members = redisTemplate.opsForSet().members(userEntity.getUsername() + "喜欢");
        ArrayList<LikeEntity> list = new ArrayList<>();

        for (Object member : members) {
            list.add((LikeEntity) member);
        }

        return processData(()->list,"操作成功",this::processException);
    }


}
