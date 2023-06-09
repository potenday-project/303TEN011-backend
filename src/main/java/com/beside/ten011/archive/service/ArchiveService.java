package com.beside.ten011.archive.service;

import com.beside.ten011.archive.controller.dto.ArchiveRequest;
import com.beside.ten011.archive.controller.dto.ArchiveResponse;
import com.beside.ten011.archive.entity.Archive;
import com.beside.ten011.archive.repository.ArchiveRepository;
import com.beside.ten011.archive.repository.ArchiveSpec;
import com.beside.ten011.exception.CustomException;
import com.beside.ten011.exception.ErrorCode;
import com.beside.ten011.user.controller.dto.MyRitualResponse;
import com.beside.ten011.user.entity.User;
import com.beside.ten011.util.PageCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ArchiveService {

    private final ArchiveRepository archiveRepository;

    @Transactional(readOnly = true)
    public Map<String, List<Integer>> getTotalCreationDates(User user) {
        return archiveRepository.getTotalCreationDates(user)
                .stream()
                .collect(Collectors.groupingBy(s -> s.split("-")[0],
                        HashMap::new,
                        Collectors.mapping(s -> Integer.parseInt(s.split("-")[1]), Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public PageCustom<ArchiveResponse> getArchiveResponsePageCustom(User user, Pageable pageable, String search, Integer year, Integer month) {
        Page<ArchiveResponse> responsePage = archiveRepository.findAll(ArchiveSpec.searchWith(user.getId(), search, year, month), pageable)
                .map(ArchiveResponse::fromEntity);
        return new PageCustom<>(responsePage.getContent(), responsePage.getPageable(), responsePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<Archive> getRandomArchive(User user) {
        return archiveRepository.findRandomArchive(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Archive> getArchive(User user, Long archiveId) {
        return archiveRepository.findByIdAndUserId(archiveId, user.getId());
    }

    @Transactional
    public Long saveArchive(User user, ArchiveRequest request) {
        Archive saveArchive = archiveRepository.save(Archive.builder()
                .user(user)
                .title(request.getTitle())
                .author(request.getAuthor())
                .content(request.getContent())
                .imageSize(request.getImageSize())
                .backgroundColor(request.getBackgroundColor())
                .fontStyle(request.getFontStyle())
                .fontColor(request.getFontColor())
                .thumbnail(request.getThumbnail())
                .build());
        return saveArchive.getId();
    }

    @Transactional
    public Long modifyArchive(User user, ArchiveRequest request, Long archiveId) {
        return getArchive(user, archiveId)
                .map(archive ->
                        archive.modify(request.getTitle(), request.getAuthor(), request.getContent(),
                                request.getImageSize(), request.getBackgroundColor(),
                                request.getFontStyle(), request.getFontColor(),
                                request.getThumbnail()
                        ).getId()
                ).orElseThrow(() -> new CustomException(ErrorCode.ARCHIVE_NOT_FOUND));
    }

    @Transactional
    public void deleteArchive(User user, Long archiveId) {
        getArchive(user, archiveId)
                .ifPresent(archiveRepository::delete);
    }

    @Transactional(readOnly = true)
    public MyRitualResponse getMyRitual(User user) {
        return MyRitualResponse.builder()
                .totalArchiveCount(archiveRepository.countByUserId(user.getId()))
                .totalBookCount(archiveRepository.countTotalBookByUser(user))
                .continuityPostDay(archiveRepository.countContinuityPostDay(user.getId()))
                .build();
    }
}
