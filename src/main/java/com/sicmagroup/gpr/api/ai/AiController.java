package com.sicmagroup.gpr.api.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

import com.sicmagroup.gpr.domain.dto.ai.ExportAffectationDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportAffectationsResponse;
import com.sicmagroup.gpr.domain.dto.ai.ExportClaimDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportClaimsResponse;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.CategorieObjetReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.CollectionChannelReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.ObjetReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.PosteReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.ProductReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.ServicePointReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportReferentielsResponse.UserReferentielDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportSolutionDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportSolutionsResponse;
import com.sicmagroup.gpr.domain.dto.ai.ExportSuggestionDto;
import com.sicmagroup.gpr.domain.dto.ai.ExportSuggestionsResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.CategorieObjetRepository;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;
import com.sicmagroup.gpr.repository.HistoriqueAffectationRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.repository.ProductRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final int DEFAULT_PAGE_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 1000;

    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final HistoriqueAffectationRepository historiqueAffectationRepository;
    private final MediaRepository mediaRepository;
    private final ClaimAudioRepository claimAudioRepository;
    private final ServicePointRepository servicePointRepository;
    private final CollectionChannelRespository collectionChannelRepository;
    private final ProductRepository productRepository;
    private final CategorieObjetRepository categorieObjetRepository;
    private final ObjetRepository objetRepository;
    private final UserRepository userRepository;
    private final PosteRepository posteRepository;
    private final SolutionRepository solutionRepository;

    public AiController(
            ClaimRepository claimRepository,
            SuggestionRepository suggestionRepository,
            HistoriqueAffectationRepository historiqueAffectationRepository,
            MediaRepository mediaRepository,
            ClaimAudioRepository claimAudioRepository,
            ServicePointRepository servicePointRepository,
            CollectionChannelRespository collectionChannelRepository,
            ProductRepository productRepository,
            CategorieObjetRepository categorieObjetRepository,
            ObjetRepository objetRepository,
            UserRepository userRepository,
            PosteRepository posteRepository,
            SolutionRepository solutionRepository) {
        this.claimRepository = claimRepository;
        this.suggestionRepository = suggestionRepository;
        this.historiqueAffectationRepository = historiqueAffectationRepository;
        this.mediaRepository = mediaRepository;
        this.claimAudioRepository = claimAudioRepository;
        this.servicePointRepository = servicePointRepository;
        this.collectionChannelRepository = collectionChannelRepository;
        this.productRepository = productRepository;
        this.categorieObjetRepository = categorieObjetRepository;
        this.objetRepository = objetRepository;
        this.userRepository = userRepository;
        this.posteRepository = posteRepository;
        this.solutionRepository = solutionRepository;
    }

    @GetMapping("/export-claims")
    public ResponseEntity<ExportClaimsResponse> exportClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) LocalDateTime updatedAfter,
            @RequestParam(required = false) LocalDateTime updatedBefore,
            @RequestParam(defaultValue = "false") boolean includeMedia,
            @RequestParam(defaultValue = "false") boolean includeAudio) {

        if (page < 0) {
            throw new IllegalArgumentException("page doit être supérieur ou égal à 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size doit être compris entre 1 et " + MAX_PAGE_SIZE);
        }

        // updatedBefore is intentionally applied after the lower cursor so the
        // synchronization can process a bounded, repeatable window.
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        Page<Claim> claims = claimRepository.findReportingClaims(
                List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION),
                updatedAfter,
                updatedBefore,
                pageable);

        List<ExportClaimDto> content = new ArrayList<>();
        LocalDateTime lastSyncAt = updatedAfter;
        for (Claim claim : claims.getContent()) {
            content.add(toExportDto(claim, includeMedia, includeAudio));
            if (claim.getUpdatedAt() != null
                    && (lastSyncAt == null || claim.getUpdatedAt().isAfter(lastSyncAt))) {
                lastSyncAt = claim.getUpdatedAt();
            }
        }

        ExportClaimsResponse response = ExportClaimsResponse.builder()
                .content(content)
                .page(claims.getNumber())
                .size(claims.getSize())
                .totalElements(claims.getTotalElements())
                .totalPages(claims.getTotalPages())
                .hasNext(claims.hasNext())
                .lastSyncAt(lastSyncAt)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-suggestions")
    public ResponseEntity<ExportSuggestionsResponse> exportSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) LocalDateTime updatedAfter,
            @RequestParam(required = false) LocalDateTime updatedBefore) {

        if (page < 0) {
            throw new IllegalArgumentException("page doit être supérieur ou égal à 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size doit être compris entre 1 et " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        Page<Suggestion> suggestions = suggestionRepository.findReportingSuggestions(
                updatedAfter,
                updatedBefore,
                pageable);

        List<ExportSuggestionDto> content = new ArrayList<>();
        LocalDateTime lastSyncAt = updatedAfter;
        for (Suggestion suggestion : suggestions.getContent()) {
            content.add(toExportSuggestionDto(suggestion));
            if (suggestion.getUpdatedAt() != null
                    && (lastSyncAt == null || suggestion.getUpdatedAt().isAfter(lastSyncAt))) {
                lastSyncAt = suggestion.getUpdatedAt();
            }
        }

        ExportSuggestionsResponse response = ExportSuggestionsResponse.builder()
                .content(content)
                .page(suggestions.getNumber())
                .size(suggestions.getSize())
                .totalElements(suggestions.getTotalElements())
                .totalPages(suggestions.getTotalPages())
                .hasNext(suggestions.hasNext())
                .lastSyncAt(lastSyncAt)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-affectations")
    public ResponseEntity<ExportAffectationsResponse> exportAffectations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) LocalDateTime updatedAfter,
            @RequestParam(required = false) LocalDateTime updatedBefore) {

        if (page < 0) {
            throw new IllegalArgumentException("page doit être supérieur ou égal à 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size doit être compris entre 1 et " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        Page<HistoriqueAffectation> affectations = historiqueAffectationRepository.findReportingAffectations(
                updatedAfter,
                updatedBefore,
                pageable);

        List<ExportAffectationDto> content = new ArrayList<>();
        LocalDateTime lastSyncAt = updatedAfter;
        for (HistoriqueAffectation affectation : affectations.getContent()) {
            content.add(toExportAffectationDto(affectation));
            if (affectation.getUpdatedAt() != null
                    && (lastSyncAt == null || affectation.getUpdatedAt().isAfter(lastSyncAt))) {
                lastSyncAt = affectation.getUpdatedAt();
            }
        }

        ExportAffectationsResponse response = ExportAffectationsResponse.builder()
                .content(content)
                .page(affectations.getNumber())
                .size(affectations.getSize())
                .totalElements(affectations.getTotalElements())
                .totalPages(affectations.getTotalPages())
                .hasNext(affectations.hasNext())
                .lastSyncAt(lastSyncAt)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-solutions")
    public ResponseEntity<ExportSolutionsResponse> exportSolutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) LocalDateTime updatedAfter,
            @RequestParam(required = false) LocalDateTime updatedBefore) {

        if (page < 0) {
            throw new IllegalArgumentException("page doit être supérieur ou égal à 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size doit être compris entre 1 et " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("id")));
        Page<Solution> solutions = solutionRepository.findReportingSolutions(
                updatedAfter,
                updatedBefore,
                pageable);

        List<ExportSolutionDto> content = new ArrayList<>();
        LocalDateTime lastSyncAt = updatedAfter;
        for (Solution solution : solutions.getContent()) {
            content.add(toExportSolutionDto(solution));
            LocalDateTime effectiveUpdated = solution.getUpdatedAt() != null ? solution.getUpdatedAt() : solution.getCreatedAt();
            if (effectiveUpdated != null
                    && (lastSyncAt == null || effectiveUpdated.isAfter(lastSyncAt))) {
                lastSyncAt = effectiveUpdated;
            }
        }

        ExportSolutionsResponse response = ExportSolutionsResponse.builder()
                .content(content)
                .page(solutions.getNumber())
                .size(solutions.getSize())
                .totalElements(solutions.getTotalElements())
                .totalPages(solutions.getTotalPages())
                .hasNext(solutions.hasNext())
                .lastSyncAt(lastSyncAt)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-referentiels")
    public ResponseEntity<ExportReferentielsResponse> exportReferentiels() {
        List<ServicePointReferentielDto> spList = servicePointRepository.findAll().stream()
                .map(sp -> ServicePointReferentielDto.builder()
                        .id(sp.getId())
                        .libelle(sp.getLibelle())
                        .description(sp.getDescription())
                        .type(sp.getType() != null ? sp.getType().name() : "AGENCE")
                        .isPrincipalAgence(sp.isPrincipalAgence())
                        .directionId(sp.getDirection_id())
                        .build())
                .collect(Collectors.toList());

        List<CollectionChannelReferentielDto> ccList = collectionChannelRepository.findAll().stream()
                .map(cc -> CollectionChannelReferentielDto.builder()
                        .id(cc.getId())
                        .libelle(cc.getLibelle())
                        .description(cc.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<ProductReferentielDto> prodList = productRepository.findAll().stream()
                .map(p -> ProductReferentielDto.builder()
                        .id(p.getId())
                        .libelle(p.getLibelle())
                        .description(p.getDescription())
                        .isDeleted(p.isDeleted())
                        .build())
                .collect(Collectors.toList());

        List<CategorieObjetReferentielDto> catList = categorieObjetRepository.findAll().stream()
                .map(c -> CategorieObjetReferentielDto.builder()
                        .id(c.getId())
                        .libelle(c.getLibelle())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<ObjetReferentielDto> objList = objetRepository.findAll().stream()
                .map(o -> ObjetReferentielDto.builder()
                        .id(o.getId())
                        .categorieId(o.getCategorie() != null ? o.getCategorie().getId() : null)
                        .libelle(o.getLibelle())
                        .risqueLevel(o.getRisqueLevel() != null ? o.getRisqueLevel().name() : null)
                        .processingTime(o.getProcessingTime())
                        .build())
                .collect(Collectors.toList());

        List<UserReferentielDto> userList = userRepository.findAll().stream()
                .map(u -> UserReferentielDto.builder()
                        .id(u.getId())
                        .code(u.getCode())
                        .firstandlastname(u.getFirstandlastname())
                        .email(u.getEmail())
                        .additionalrole(u.getAdditionalrole() != null ? u.getAdditionalrole().name() : null)
                        .servicePointId(u.getServicePoint() != null ? u.getServicePoint().getId() : null)
                        .posteId(u.getPoste() != null ? u.getPoste().getId() : null)
                        .build())
                .collect(Collectors.toList());

        List<PosteReferentielDto> posteList = posteRepository.findAll().stream()
                .map(p -> PosteReferentielDto.builder()
                        .id(p.getId())
                        .libelle(p.getLibelle())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        ExportReferentielsResponse response = ExportReferentielsResponse.builder()
                .servicePoints(spList)
                .collectionChannels(ccList)
                .products(prodList)
                .categories(catList)
                .objets(objList)
                .users(userList)
                .postes(posteList)
                .build();
        return ResponseEntity.ok(response);
    }

    private ExportClaimDto toExportDto(Claim claim, boolean includeMedia, boolean includeAudio) {
        String solution = "";
        if (claim.getSolutions() != null && !claim.getSolutions().isEmpty()) {
            Solution latest = claim.getSolutions().get(claim.getSolutions().size() - 1);
            solution = latest.getContent();
        }

        String category = "";
        String motif = "";
        if (claim.getObjet() != null) {
            motif = claim.getObjet().getLibelle();
            if (claim.getObjet().getCategorie() != null) {
                category = claim.getObjet().getCategorie().getLibelle();
            }
        }

        String servicePoint = claim.getServicePoint() != null
                ? claim.getServicePoint().getLibelle()
                : "";
        String channel = claim.getCollectionChannel() != null
                ? claim.getCollectionChannel().getLibelle()
                : "";
        String product = claim.getProduct() != null
                ? claim.getProduct().getLibelle()
                : "";

        LocalDateTime resolvedAt = null;
        if (claim.getSolutions() != null && !claim.getSolutions().isEmpty()) {
            for (int i = claim.getSolutions().size() - 1; i >= 0; i--) {
                Solution sol = claim.getSolutions().get(i);
                if (sol.getStatus() == SolutionStatus.APPROVED && sol.getApprouvedAt() != null) {
                    resolvedAt = sol.getApprouvedAt();
                    break;
                }
            }
            if (resolvedAt == null) {
                Solution latest = claim.getSolutions().get(claim.getSolutions().size() - 1);
                if (latest.getApprouvedAt() != null) {
                    resolvedAt = latest.getApprouvedAt();
                } else if (claim.getStatus() == ClaimStatus.TREAT
                        || claim.getStatus() == ClaimStatus.SATISFIED
                        || claim.getStatus() == ClaimStatus.UNSATISFIED
                        || claim.getStatus() == ClaimStatus.PARTIAL_SATISFIED
                        || claim.getStatus() == ClaimStatus.CLASSED) {
                    resolvedAt = latest.getUpdatedAt() != null ? latest.getUpdatedAt() : claim.getUpdatedAt();
                }
            }
        }

        Long objetId = claim.getObjet() != null ? claim.getObjet().getId() : null;
        Long productId = claim.getProduct() != null ? claim.getProduct().getId() : null;
        Long servicePointId = claim.getServicePoint() != null ? claim.getServicePoint().getId() : null;
        Long collectionChannelId = claim.getCollectionChannel() != null ? claim.getCollectionChannel().getId() : null;
        Long collectorId = claim.getCollector() != null ? claim.getCollector().getId() : null;

        List<ExportClaimDto.MediaDto> mediaDtos = includeMedia
                ? mediaRepository.findByClaimId(claim.getId()).stream()
                        .map(media -> ExportClaimDto.MediaDto.builder()
                                .id(media.getId())
                                .name(media.getName())
                                .path(media.getPath())
                                .build())
                        .toList()
                : List.of();

        List<ExportClaimDto.AudioDto> audioDtos = includeAudio
                ? claimAudioRepository.findByClaimId(claim.getId()).stream()
                        .map(audio -> ExportClaimDto.AudioDto.builder()
                                .id(audio.getId())
                                .name(audio.getName())
                                .path(audio.getPath())
                                .build())
                        .toList()
                : List.of();

        return ExportClaimDto.builder()
                .id(claim.getId())
                .code(claim.getCode())
                .codeClient(claim.getCodeClient())
                .claimType(claim.getType().name())
                .texte_plainte(claim.getContent())
                .texte_solution(solution)
                .statut_final(claim.getStatus() == null ? null : claim.getStatus().name())
                .objet_categorie(category)
                .motif_reclamation(motif)
                .point_service_indexe(servicePoint)
                .modalite_depot(channel)
                .produit_service(product)
                .date_creation(claim.getCreatedAt())
                .receiptDateTime(claim.getReceiptDateTime())
                .slaDueAt(calculateSlaDueAt(claim))
                .updatedAt(claim.getUpdatedAt())
                .affectedAt(claim.getAffectedAt())
                .resolvedAt(resolvedAt)
                .objetId(objetId)
                .productId(productId)
                .servicePointId(servicePointId)
                .collectionChannelId(collectionChannelId)
                .collectorId(collectorId)
                .aiUrgency(claim.getAiUrgency())
                .aiSentiment(claim.getAiSentiment())
                .aiSuggestedCategory(claim.getAiSuggestedCategory())
                .aiSuggestedMotif(claim.getAiSuggestedMotif())
                .aiSummary(claim.getAiSummary())
                .aiFeedbackStatus(claim.getAiFeedbackStatus())
                .aiFinalCategory(claim.getAiFinalCategory())
                .aiFinalMotif(claim.getAiFinalMotif())
                .aiFinalUrgency(claim.getAiFinalUrgency())
                .aiRetainedSolution(claim.getAiRetainedSolution())
                .aiFeedbackAgentId(claim.getAiFeedbackAgentId())
                .aiFeedbackSatisfactionStatus(claim.getAiFeedbackSatisfactionStatus())
                .aiFeedbackDate(claim.getAiFeedbackDate())
                .aiQualityScore(claim.getAiQualityScore())
                .riskLevel(claim.getObjet() != null && claim.getObjet().getRisqueLevel() != null
                        ? claim.getObjet().getRisqueLevel().name()
                        : null)
                .agence(servicePoint)
                .equipe(claim.getCrew())
                .medias(mediaDtos)
                .audios(audioDtos)
                .build();
    }

    private ExportSuggestionDto toExportSuggestionDto(Suggestion suggestion) {
        String servicePoint = suggestion.getServiceIndexe() != null
                ? suggestion.getServiceIndexe().getLibelle()
                : null;
        String channel = suggestion.getCanal() != null
                ? suggestion.getCanal().getLibelle()
                : null;
        String product = suggestion.getProduit() != null
                ? suggestion.getProduit().getLibelle()
                : null;

        Long productId = suggestion.getProduit() != null ? suggestion.getProduit().getId() : null;
        Long servicePointId = suggestion.getServiceIndexe() != null ? suggestion.getServiceIndexe().getId() : null;
        Long collectionChannelId = suggestion.getCanal() != null ? suggestion.getCanal().getId() : null;
        Long collectorId = suggestion.getCollecteur() != null ? suggestion.getCollecteur().getId() : null;

        return ExportSuggestionDto.builder()
                .id(suggestion.getId())
                .code(suggestion.getCode())
                .codeClient(suggestion.getCodeClient())
                .clientFirstAndLastName(suggestion.getClientFirstAndLastName())
                .tel(suggestion.getTel())
                .content(suggestion.getContent())
                .status(suggestion.getStatus() != null ? suggestion.getStatus().name() : null)
                .canal(channel)
                .serviceIndexe(servicePoint)
                .produit(product)
                .commentaire(suggestion.getCommentaire())
                .aiSummary(suggestion.getAiSummary())
                .aiSentiment(suggestion.getAiSentiment())
                .aiUrgency(suggestion.getAiUrgency())
                .impactLevel(suggestion.getAiUrgency() != null ? suggestion.getAiUrgency() : "MOYEN")
                .origine("EXTERNE")
                .objetId(null)
                .productId(productId)
                .servicePointId(servicePointId)
                .collectionChannelId(collectionChannelId)
                .collectorId(collectorId)
                .createdAt(suggestion.getCreatedAt())
                .receiptDateTime(suggestion.getReceiptDateTime())
                .updatedAt(suggestion.getUpdatedAt())
                .treatAt(suggestion.getTreatAt())
                .build();
    }

    private ExportAffectationDto toExportAffectationDto(HistoriqueAffectation h) {
        return ExportAffectationDto.builder()
                .id(h.getId())
                .reclamationId(h.getReclamationId())
                .suggestionId(null)
                .codePlainte(h.getCodePlainte())
                .typePlainte(h.getTypePlainte() != null ? h.getTypePlainte().name() : null)
                .nomAgent(h.getNomAgent())
                .emailAgent(h.getEmailAgent())
                .telephoneAgent(h.getTelephoneAgent())
                .affecteurId(h.getAffecteurId())
                .nomAffecteur(h.getNomAffecteur())
                .emailAffecteur(h.getEmailAffecteur())
                .dateAffectation(h.getDateAffectation())
                .delaiJours(h.getDelaiJours())
                .dateFinAffectation(h.getDateFinAffectation())
                .affectationPrecedenteId(h.getAffectationPrecedenteId())
                .lastNotification(h.getLastNotification())
                .mailEnvoye(h.getMailEnvoye())
                .smsEnvoye(h.getSmsEnvoye())
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }

    private ExportSolutionDto toExportSolutionDto(Solution s) {
        ExportSolutionDto.ExportSatisfactionMeasureDto smDto = null;
        if (s.getSatisfactionMeasure() != null) {
            SatisfactionMeasure sm = s.getSatisfactionMeasure();
            smDto = ExportSolutionDto.ExportSatisfactionMeasureDto.builder()
                    .id(sm.getId())
                    .solutionId(s.getId())
                    .measurerId(sm.getMeasurer() != null ? sm.getMeasurer().getId() : null)
                    .status(sm.getStatus() != null ? sm.getStatus().name() : null)
                    .commentaire(sm.getCommentaire())
                    .measureDateTime(sm.getMeasureDateTime() != null ? sm.getMeasureDateTime() : (s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt()))
                    .build();
        }

        LocalDateTime updatedAt = s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt();
        String commentaire = s.getCommentaire();
        if (commentaire == null || commentaire.isBlank()) {
            commentaire = s.getContent();
        }

        return ExportSolutionDto.builder()
                .id(s.getId())
                .claimId(s.getClaim() != null ? s.getClaim().getId() : null)
                .authorId(s.getAuthor() != null ? s.getAuthor().getId() : null)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .content(s.getContent())
                .commentaire(commentaire)
                .motifDesaprobation(s.getMotifDesaprobation())
                .approverId(s.getApprouver() != null ? s.getApprouver().getId() : null)
                .approvedAt(s.getApprouvedAt())
                .unapprovedAt(s.getUnApprouvedAt())
                .isAiProposed(s.getIsAiProposed())
                .aiSelectedSolution(s.getAiSelectedSolution())
                .createdAt(s.getCreatedAt())
                .updatedAt(updatedAt)
                .satisfactionMeasure(smDto)
                .build();
    }

    private LocalDateTime calculateSlaDueAt(Claim claim) {
        if (claim.getReceiptDateTime() == null || claim.getObjet() == null) {
            return null;
        }
        return claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
    }
}
