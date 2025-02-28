package alkong_dalkong.backend.Common.Filter;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.web.filter.GenericFilterBean;

import alkong_dalkong.backend.Common.Util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LogoutFilter extends GenericFilterBean {
    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // /logout경로로 post요청을 통해 로그아웃이 호출되었는지 검증
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^\\/user\\/logout$")) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 refresh토큰 추출
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        // refresh토큰 null check
        if (refresh == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // refresh토큰 만료 여부 check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            // response status code
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            // response status code
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Refresh 토큰값 null, 생명주기 0인 Cookie 생성
        // redis나 mysql로 토큰을 관리하는 방법 대신으로 만료 토큰을 바로 만료시켜서 탈취 대응
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print("로그아웃이 완료되었습니다.");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}