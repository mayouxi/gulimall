package com.sjy.gulimall.seckill.intercept;

import com.sjy.common.to.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

import static com.sjy.common.constant.AuthServerConstant.LOGIN_USER;

/**
 * @author zr
 * @date 2022/1/10 15:13
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/kill", uri);
        // 只有秒杀需要拦截，其他直接放行
        if (match) {
            HttpSession session = request.getSession();
            //获取登录的用户信息
            MemberResponseVo attribute = (MemberResponseVo) session.getAttribute(LOGIN_USER);
            if (attribute != null) {
                //把登录后用户的信息放在ThreadLocal里面进行保存
                loginUser.set(attribute);
                return true;
            } else {
                //未登录，返回登录页面
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("<script>alert('请先进行登录，再进行后续操作！');location.href='http://auth.gulimall.com/login.html'</script>");
                // session.setAttribute("msg", "请先进行登录");
                // response.sendRedirect("http://auth.gulimall.com/login.html");
                return false;
            }
        }
        return true;
    }
}

