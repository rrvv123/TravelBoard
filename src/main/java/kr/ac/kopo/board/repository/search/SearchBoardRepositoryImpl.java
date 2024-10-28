package kr.ac.kopo.board.repository.search;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import kr.ac.kopo.board.entity.Board;
import kr.ac.kopo.board.entity.QBoard;
import kr.ac.kopo.board.entity.QMember;
import kr.ac.kopo.board.entity.QReply;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class SearchBoardRepositoryImpl extends QuerydslRepositorySupport implements SearchBoardRepository {
    public SearchBoardRepositoryImpl(){
        super(Board.class);
    }

    @Override
    public Board search1(){
        log.info("search1() 메소드 호출됨.");
        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);

        jpqlQuery.leftJoin(member).on(board.writer.eq(member)); // (left join)왼쪽에 board가 있어야 board의 내용(board안에 있는 member도)이 다 나옴.
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board)).groupBy(board, reply); // 마리아DB가 아닌 경우 groupBy() 꼭 사용해야함. 마리아DB는 groupBy 생략가능 혹은 제일 큰 객체인 board만 작성해도 됨.

//        jpqlQuery.select(board, member.email, reply.count()).groupBy(board, member, reply);
        JPQLQuery<Tuple> tuple = jpqlQuery.select(board, member.email, reply.count()); // 튜플 적용
        tuple.groupBy(board, member, reply);

        log.info("====================================");
        log.info(jpqlQuery);
        log.info("====================================");

//        JPQL 설정 방법
//        List<Board> result = jpqlQuery.fetch();
        List<Tuple> result = tuple.fetch(); // 튜블 설정 방법

        log.info("====================================");
        log.info(result);
        log.info("====================================");


        return null;
    }

    @Override
    public Page<Object[]> searchPage(String type, String keyword, Pageable pageable) {
        log.info("searchPage() 메소드 호출됨.");

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);

        jpqlQuery.leftJoin(member).on(board.writer.eq(member)); // (left join)왼쪽에 board가 있어야 board의 내용(board안에 있는 member도)이 다 나옴.
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board)).groupBy(board, reply); // 마리아DB가 아닌 경우 groupBy() 꼭 사용해야함. 마리아DB는 groupBy 생략가능 혹은 제일 큰 객체인 board만 작성해도 됨.

        JPQLQuery<Tuple> tuple = jpqlQuery.select(board, member, reply.count()); // 튜플 적용, ## member.email을 전체 선택가능하게 member로 수정함.
//        tuple.groupBy(board, member, reply);

        BooleanBuilder booleanBuilder = new BooleanBuilder(); // 기본 생성자 생성
        BooleanExpression expression = board.bno.gt(0L); // gt: 오른쪽에 있는거 보다 크다, lt: 오른쪽에 있는거 보다 작다.
        booleanBuilder.and(expression);

        if (type != null){
            String[] typeArr = type.split("");
            BooleanBuilder conditionbuilder = new BooleanBuilder();

            for (String t : typeArr){
                switch (t){
                    case "t":
                        conditionbuilder.or(board.title.contains(keyword));
                        break;
                    case "w":
                        conditionbuilder.or(member.email.contains(keyword));
                        break;
                    case "c":
                        conditionbuilder.or(board.content.contains(keyword));
                        break;
                } // enf switch
            } // end for
            booleanBuilder.and(conditionbuilder);
        } // end if

        tuple.where(booleanBuilder);
//        Sorting(Order by)
        Sort sort = pageable.getSort();

//        정렬 처리
        sort.stream().forEach(order -> {
            Order direction = order.isAscending()? Order.ASC:Order.DESC;
            String prop = order.getProperty();

            PathBuilder orderByExpression = new PathBuilder(Board.class, "board");

            tuple.orderBy(new OrderSpecifier(direction, orderByExpression.get(prop))); // <Comparable> 이 표현식은 생략해도 됨.
        });

        tuple.groupBy(board, member);


//        페이지 처리에 필요한 값(offset, limit) 설정
        tuple.offset(pageable.getOffset());
        tuple.limit(pageable.getPageSize());

        List<Tuple> result = tuple.fetch(); // 실행

        log.info(result);

        long count = tuple.fetchCount();

        log.info("실행된 행(tuple)의 개수:" + count);

        return new PageImpl<Object[]>(result.stream().map(t -> t.toArray()).collect(Collectors.toList()), pageable, count);
    }
}
