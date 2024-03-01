package com.dhu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dhu.entity.Paper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PaperService {
    //获取知识库的Paper列表
    IPage<Paper> queryPapers(int current, int size, Integer kbId);

    //上传论文
    boolean uploadPaper(Integer kbId,Integer builderId, MultipartFile files);

    //删除论文
    boolean deletePaper(Integer paperId);

    //删除知识库中所有论文
    boolean deletePaperByKb(Integer kbId);

    //修改论文
    boolean updatePaper(Paper paper);

    //批量删除一个知识库中的论文
    boolean deletePapersByKb(List<Integer> paperIds, Integer kbId);

    //查询知识库中论文数量
    long countTeamKnowledgeBases(Integer kbId);
}
