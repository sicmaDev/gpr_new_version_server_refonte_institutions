package com.sicmagroup.gpr.api.synchro;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.Media.MediaResponse;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.ProposedSolutionRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.api.denunciation.SaveDenunRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.api.suggestion.TreatSuggestionRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.SatisfactionMeasureDto;
import com.sicmagroup.gpr.domain.dto.SolutionDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.CollectionChannelResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ExternalRecourseResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.LanguageResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ProductResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;
import com.sicmagroup.gpr.service.suggestion.SuggestionServiceImpl;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Client;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sync")
public class SynchronerController {

	private final ClaimServiceImpl claimServiceImpl;
	private final AuthenticationServiceImpl authService;
	private final SuggestionServiceImpl suggestionServiceImpl;
	private final SuggestionServiceImpl service;
	private final ModelMapper modelMapper;

	@PostMapping(value = "/claim")
	public ResponseEntity<ApiResponseDto> syncClaimEntity(@RequestBody SyncAllOffline allDataToSync) {
		ApiResponseDto apiResponseDto;
		UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();

		try {
			syncClaimList(allDataToSync.claims);
			syncDenunList(allDataToSync.denuns);
			syncSuggestionList(allDataToSync.suggestions);
		} catch (Exception e) {
			System.out.println("Error happened " + e.getMessage());
		}

		apiResponseDto = ApiResponseDto
				.builder()
				.content("Response")
				.status(true)
				.build();

		return ResponseEntity.ok(apiResponseDto);
	}

	@PostMapping(value = "/denun")
	public ResponseEntity<ApiResponseDto> syncDenunEntity(@RequestBody List<SyncClaimRequest> syncClaimRequests) {
		ApiResponseDto apiResponseDto;
		DenunRequest claimRequest;
		SaveDenunRequest saveRequest;
		Claim claim = Claim.builder().build();
		UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();
		User connectedUser = User.builder().build();
		List<Claim> claims = new ArrayList<>();
		try {
			connectedUser = authService.getByEmail(collectorDetails.getUsername());
		} catch (Exception e) {
			apiResponseDto = ApiResponseDto
					.builder()
					.status(false)
					.content(ErrorResponse.builder().message("Utilisateur introuvable")
							.title("NOT FOUND EXCEPTION")
							.build())
					.build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
		}
		for (SyncClaimRequest syncClaimRequest : syncClaimRequests) {

			claimRequest = DenunRequest
					.builder()
					.status(syncClaimRequest.getStatus())
					.code(syncClaimRequest.getCode())
					.createdAt(syncClaimRequest.getCreatedAt())
					// .clientFirstAndLastName(syncClaimRequest.getClientFirstAndLastName())
					// .address(syncClaimRequest.getAddress())
					.collectionChannelId(syncClaimRequest.getCollectionChannelId())
					.collectorId(syncClaimRequest.getCollectorId())
					.content(syncClaimRequest.getContent())
					// .crew(syncClaimRequest.getCrew())
					// .folderCode(syncClaimRequest.getFolderCode())
					// .gender(syncClaimRequest.getGender())
					.languageId(syncClaimRequest.getLanguageId())
					.objetId(syncClaimRequest.getObjetId())
					// .phone(syncClaimRequest.getPhone())
					.productId(syncClaimRequest.getProductId())
					.receiptDateTime(syncClaimRequest.getReceiptDateTime())
					.servicePointId(syncClaimRequest.getServicePointId())
					.build();
			if (syncClaimRequest.getId() != null) {
				claimRequest.setId(syncClaimRequest.getId());
			}
			saveRequest = SaveDenunRequest
					.builder()
					.claimRequest(claimRequest)
					.files(syncClaimRequest.getFiles())
					.build();

			try {
				if (claimRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
					claimServiceImpl.saveTempDenunOffline(saveRequest, ClaimType.DENUNCIACION);
				} else {
					claimServiceImpl.saveDenunOffline(saveRequest, ClaimType.DENUNCIACION);
				}

				// if (syncClaimRequest.getSolution() != null) {
				// ProposedSolutionRequest proposedSolutionRequest = ProposedSolutionRequest
				// .builder()
				// .claimId(claim.getId())
				// .commentaire(syncClaimRequest.getCommentaire())
				// .solution(syncClaimRequest.getSolution())
				// .treatorId(connectedUser.getId())

				// .build();
				// claim = claimServiceImpl.treatClaim(claim, connectedUser,
				// proposedSolutionRequest);

				// if (syncClaimRequest.getSatisfactionStatus() != null) {
				// claim = claimServiceImpl.measureClaim(claim,
				// claim.getSolutions().get(0), connectedUser,
				// syncClaimRequest.getSatisfactionStatus());
				// }

				// }

				claims.add(claim);

			} catch (Exception e) {
				apiResponseDto = ApiResponseDto
						.builder()
						.content(ErrorResponse.builder().title("Une erreur est survenue")
								.message("Impossible de sauvegarder la rélamation de "
										+ syncClaimRequest
												.getClientFirstAndLastName()))
						.status(false)
						.build();
				// return ResponseEntity.ok(apiResponseDto);
			}

		}
		apiResponseDto = ApiResponseDto
				.builder()
				.content(claims)
				.status(true)
				.build();

		return ResponseEntity.ok(apiResponseDto);
	}

	@PostMapping(value = "/suggestion")
	public ResponseEntity<ApiResponseDto> syncSuggestionEntity(
			@RequestBody List<SyncSuggestionRequest> syncSuggestionRequests) {
		ApiResponseDto apiResponseDto;
		SuggestionRequest suggestionRequest;
		Suggestion suggestion = Suggestion.builder().build();
		UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();
		User connectedUser = User.builder().build();
		try {
			connectedUser = authService.getByEmail(collectorDetails.getUsername());
		} catch (Exception e) {
			apiResponseDto = ApiResponseDto
					.builder()
					.status(false)
					.content(ErrorResponse.builder().message("Utilisateur introuvable")
							.title("NOT FOUND EXCEPTION")
							.build())
					.build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
		}

		List<Suggestion> suggestions = new ArrayList<>();
		for (SyncSuggestionRequest syncSuggestionRequest : syncSuggestionRequests) {
			suggestionRequest = SuggestionRequest
					.builder()
					.clientFirstAndLastName(syncSuggestionRequest.getClientFirstAndLastName())
					.address(syncSuggestionRequest.getAddress())
					.collectionChannelId(syncSuggestionRequest.getCollectionChannelId())
					.collectorId(syncSuggestionRequest.getCollectorId())
					.content(syncSuggestionRequest.getContent())
					.crew(syncSuggestionRequest.getCrew())
					.folderCode(syncSuggestionRequest.getFolderCode())
					.gender(syncSuggestionRequest.getGender())
					.languageId(syncSuggestionRequest.getLanguageId())
					.objetId(syncSuggestionRequest.getObjetId())
					.phone(syncSuggestionRequest.getPhone())
					.productId(syncSuggestionRequest.getProductId())
					.receiptDateTime(syncSuggestionRequest.getReceiptDateTime())
					.servicePointId(syncSuggestionRequest.getServicePointId())
					.build();
			if (syncSuggestionRequest.getId() != null) {
				suggestionRequest.setId(syncSuggestionRequest.getId());
			}
			SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
					.builder()
					.files(syncSuggestionRequest.getFiles())
					.suggestionRequest(suggestionRequest)
					.build();
			try {
				suggestion = suggestionServiceImpl.saveSuggestion(suggestionAddRequest,
						ClaimStatus.SAVED);
				TreatSuggestionRequest treatSuggestionRequest = TreatSuggestionRequest
						.builder()
						.accepted(syncSuggestionRequest.isAccepted())
						.commentaire(syncSuggestionRequest.getCommentaire())
						.treatorId(connectedUser.getId())
						.id(suggestion.getId())
						.build();
				suggestion = suggestionServiceImpl.treatSuggestion(suggestion, connectedUser,
						treatSuggestionRequest);
			} catch (Exception e) {
				apiResponseDto = ApiResponseDto
						.builder()
						.content(ErrorResponse.builder().title("Une erreur est survenue")
								.message("Impossible de sauvegarder la suggestion de "
										+ syncSuggestionRequest
												.getClientFirstAndLastName()))
						.status(false)
						.build();
				return ResponseEntity.ok(apiResponseDto);
			}
			suggestions.add(suggestion);
		}

		apiResponseDto = ApiResponseDto
				.builder()
				.content(suggestion)
				.status(true)
				.build();

		return ResponseEntity.ok(apiResponseDto);
	}

	@GetMapping(value = "/allList")
	public ResponseEntity<ApiResponseDto> getAllList() {
		ApiResponseDto apiResponseDto;
		UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();
		User connectedUser = User.builder().build();
		List<Claim> allClaims = new ArrayList<>();
		try {
			connectedUser = authService.getByEmail(collectorDetails.getUsername());
		} catch (Exception e) {
			apiResponseDto = ApiResponseDto
					.builder()
					.status(false)
					.content(ErrorResponse.builder().message("Utilisateur introuvable")
							.title("NOT FOUND EXCEPTION")
							.build())
					.build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
		}
		allClaims = claimServiceImpl.getAll(ClaimType.CLAIM);

		List<Claim> tmpClaims = new ArrayList<>();
		System.out.println("Size " + allClaims.size());
		if (!connectedUser.getAdditionalrole().equals(Role.PILOTE)
				&& !connectedUser.getAdditionalrole().equals(Role.DE)) {

			for (Claim claim : allClaims) {
				if (((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| (claim.getStatus() != ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| claim.getTreatmentAffectedTo() == connectedUser)) {
					// allClaims.remove(claim);
					tmpClaims.add(claim);
				}
			}

		} else {
			for (Claim claim : allClaims) {
				if ((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| claim.getStatus() != ClaimStatus.TEMP_SAVED) {
					tmpClaims.add(claim);
				}

			}
		}

		allClaims = tmpClaims;

		// if (!connectedUser.canSeeAll()) {// ! habilité H14
		// for (Claim claim : allClaims) {

		// if (claim.getCollector() == connectedUser
		// || claim.getTreatmentAffectedTo() == connectedUser) {
		// // allClaims.remove(claim);
		// tmpClaims.add(claim);
		// }
		// }
		// } else {
		// for (Claim claim : allClaims) {
		// if ((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() ==
		// connectedUser)
		// || (claim.getStatus() != ClaimStatus.TEMP_SAVED)) {
		// // allClaims.remove(claim);
		// tmpClaims.add(claim);
		// }

		// }
		// }

		// System.out.println("Size end "+allClaims.size());
		// allClaims = tmpClaims;
		List<Claim> allDenun = claimServiceImpl.getAll(ClaimType.DENUNCIACION);
		List<Claim> tmpDenun = new ArrayList<>();
		if (!connectedUser.getAdditionalrole().equals(Role.PILOTE)
				&& !connectedUser.getAdditionalrole().equals(Role.DE)) {

			for (Claim claim : allDenun) {
				if (((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| (claim.getStatus() != ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| claim.getTreatmentAffectedTo() == connectedUser)) {
					// allClaims.remove(claim);
					tmpDenun.add(claim);
				}
			}

		} else {
			for (Claim claim : allDenun) {
				if ((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() == connectedUser)
						|| claim.getStatus() != ClaimStatus.TEMP_SAVED) {
					tmpDenun.add(claim);
				}

			}
		}

		allDenun = tmpDenun;
		// if (!connectedUser.canSeeAll()) {// ! habilité H14
		// for (Claim claim : allDenun) {
		// if (claim.getCollector() == connectedUser
		// || claim.getTreatmentAffectedTo() == connectedUser) {
		// // allDenun.remove(claim);
		// tmpDenun.add(claim);
		// }
		// }
		// } else {
		// for (Claim claim : allDenun) {
		// if ((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollector() ==
		// connectedUser)
		// || (claim.getStatus() != ClaimStatus.TEMP_SAVED)) {
		// // allDenun.remove(claim);
		// tmpDenun.add(claim);
		// }
		// }
		// }
		// allDenun = tmpDenun;
		List<Suggestion> suggestions = service.getAll();
		List<Suggestion> tmpSuggestions = new ArrayList<>();
		if (!connectedUser.getAdditionalrole().equals(Role.PILOTE)
				&& !connectedUser.getAdditionalrole().equals(Role.DE)) {

			for (Suggestion claim : suggestions) {
				if ((claim.getStatus() == ClaimStatus.TEMP_SAVED && claim.getCollecteur() == connectedUser)
						|| (claim.getStatus() != ClaimStatus.TEMP_SAVED && claim.getCollecteur() == connectedUser)) {
					// allClaims.remove(claim);
					tmpSuggestions.add(claim);
				}
			}

		} else {
			for (Suggestion claim : suggestions) {
				if (claim.getStatus() == ClaimStatus.TEMP_SAVED) {
					if ((claim.getCollecteur() == connectedUser)) {
						// allClaims.remove(claim);
						tmpSuggestions.add(claim);
					}
				} else {
					tmpSuggestions.add(claim);
				}

			}
		}
		suggestions = tmpSuggestions;
		// for (Suggestion suggestion : suggestions) {
		// if (suggestion.getStatus() == ClaimStatus.TEMP_SAVED &&
		// suggestion.getCollecteur() == connectedUser) {
		// tmpSuggestions.add(suggestion);
		// } else if (suggestion.getStatus() != ClaimStatus.TEMP_SAVED) {
		// tmpSuggestions.add(suggestion);
		// }
		// }
		// suggestions = tmpSuggestions;

		SyncAllGet syncAllGet = SyncAllGet
				.builder()
				.claimDto(allClaims.stream().map(this::convertToDto).collect(Collectors.toList()))
				.denunDto(allDenun.stream().map(this::convertToDto).collect(Collectors.toList()))
				.suggestionDto(suggestions.stream().map(this::convertToDto)
						.collect(Collectors.toList()))
				.build();
		apiResponseDto = ApiResponseDto
				.builder()
				.content(syncAllGet)
				.status(true)
				.build();
		return ResponseEntity.ok(apiResponseDto);
	}

	private MediaResponse convertToResponse(Media media) {
		MediaResponse mediaResponse = modelMapper.map(media, MediaResponse.class);
		mediaResponse.setSize(media.getSize());
		return mediaResponse;
	}

	private UserResponse convertToResponse(User user) {
		UserResponse userResponse = modelMapper.map(user, UserResponse.class);
		return userResponse;
	};

	private ClaimDto convertToDto(Claim claim) {
		ClaimDto claimDto = modelMapper.map(claim, ClaimDto.class);
		if (claim.getProduct() != null) {
			claimDto.setProduct(convertToResponse(claim.getProduct()));
		}
		if (claim.getCollectionChannel() != null) {
			claimDto.setCollectionChannel(convertToResponse(claim.getCollectionChannel()));
		}

		if (claim.getObjet() != null) {
			claimDto.setObjet(convertToResponse(claim.getObjet()));
		}

		if (claim.getServicePoint() != null) {
			claimDto.setServicePoint(convertToResponse(claim.getServicePoint()));
		}

		if (claim.getLanguage() != null) {
			claimDto.setLanguage(convertToResponse(claim.getLanguage()));
		}

		if (claim.getCollector() != null) {
			claimDto.setCollector(convertToResponse(claim.getCollector()));
		}

		// if (claim.getMedias() != null) {
		// claimDto.setMedias(claim.getMedias());
		// }

		if (claim.getSolutions() != null) {
			claimDto.setSolutionDtos(
					claim.getSolutions().stream().map(this::convertToDto)
							.collect(Collectors.toList()));

			Collections.reverse(claimDto.getSolutionDtos());

		}

		if (claim.getExternalRecourses() != null) {
			claimDto.setExternalRecourses(
					claim.getExternalRecourses().stream().map(this::convertToResponse)
							.collect(Collectors.toList()));
		}

		if (claim.getReceiptDateTime() != null) {
			claimDto.setReceiptDateTime(claimDto.convertDate(claim.getReceiptDateTime()));
		}

		// if (claim.getCreatedAt() != null) {
		// claimDto.setCreatedAt(claimDto.convertDate(claim.getCreatedAt()));
		// }

		if (claim.getUpdatedAt() != null) {
			claimDto.setUpdatedAt(claimDto.convertDate(claim.getUpdatedAt()));
		}

		// if (claim.getAffectedAt() != null) {
		// claimDto.setAffectedAt(claimDto.convertDate(claim.getAffectedAt()));
		// }
		return claimDto;
	}

	private SolutionDto convertToDto(Solution solution) {
		SolutionDto solutionDto = modelMapper.map(solution, SolutionDto.class);
		if (solution.getSatisfactionMeasure() != null) {
			solutionDto.setSatisfactionMeasureDto(convertToDto(solution.getSatisfactionMeasure()));
		}

		if (solution.getAuthor() != null) {
			solutionDto.setAuthor(convertToResponse(solution.getAuthor()));
		}

		if (solution.getApprouver() != null) {
			solutionDto.setApprouver(convertToResponse(solution.getApprouver()));
		}

		if (solution.getUnApprouver() != null) {
			solutionDto.setUnApprouver(convertToResponse(solution.getUnApprouver()));
		}

		return solutionDto;
	}

	private SatisfactionMeasureDto convertToDto(SatisfactionMeasure satisfactionMeasure) {
		SatisfactionMeasureDto satisfactionMeasureDto = modelMapper.map(satisfactionMeasure,
				SatisfactionMeasureDto.class);
		satisfactionMeasureDto.setMeasurer(convertToResponse(satisfactionMeasure.getMeasurer()));
		return satisfactionMeasureDto;
	}

	private ServicePointResponse convertToResponse(ServicePoint servicepoint1) {
		ServicePointResponse servicePointResponse = modelMapper.map(servicepoint1, ServicePointResponse.class);
		return servicePointResponse;
	}

	private ProductResponse convertToResponse(Product product) {
		ProductResponse productResponse = modelMapper.map(product, ProductResponse.class);
		return productResponse;
	}

	private ObjetResponse convertToResponse(Objet objet) {
		ObjetResponse objetResponse = modelMapper.map(objet, ObjetResponse.class);
		if (objet.getExistingSolutions() != null) {
			objetResponse.setExistingSolutions(
					objet.getExistingSolutions().stream().map(this::convertToResponse).collect(Collectors.toList()));
		}

		if (objet.getCategorie() != null) {
			objetResponse.setCategorie(null);
			objetResponse.setCategorie(convertToDto(objet.getCategorie()));
		}

		return objetResponse;
	}

	private ExistingSolutionResponse convertToResponse(ExistingSolution exSolution) {
		ExistingSolutionResponse existingSolutionResponse = modelMapper.map(exSolution, ExistingSolutionResponse.class);
		return existingSolutionResponse;
	}

	private CategorieObjetDto convertToDto(CategorieObjet categorieObjet) {
		CategorieObjetDto dto = modelMapper.map(categorieObjet, CategorieObjetDto.class);
		return dto;
	}

	private LanguageResponse convertToResponse(Language language) {
		LanguageResponse languageResponse = modelMapper.map(language, LanguageResponse.class);
		return languageResponse;
	}

	private CollectionChannelResponse convertToResponse(CollectionChannel collectionChannel) {
		CollectionChannelResponse collectionChannelResponse = modelMapper.map(collectionChannel,
				CollectionChannelResponse.class);
		return collectionChannelResponse;
	}

	private ExternalRecourseResponse convertToResponse(ExternalRecourse externalRecourse) {
		ExternalRecourseResponse externalRecourseResponse = modelMapper.map(externalRecourse,
				ExternalRecourseResponse.class);
		return externalRecourseResponse;
	}

	private SuggestionDto convertToDto(Suggestion suggestion) {
		SuggestionDto suggestionDto = modelMapper.map(suggestion, SuggestionDto.class);
		return suggestionDto;
	}

	private void syncClaimList(List<SyncClaimRequest> syncClaimRequests) throws Exception {
		ClaimRequest claimRequest;
		SaveRequest saveRequest;
		// Claim claim = Claim.builder().build();

		for (SyncClaimRequest syncClaimRequest : syncClaimRequests) {
			claimRequest = ClaimRequest
					.builder()
					.code(syncClaimRequest.getCode())
					.createdAt(syncClaimRequest.getCreatedAt())
					.status(syncClaimRequest.getStatus())
					.clientFirstAndLastName(syncClaimRequest.getClientFirstAndLastName())
					.address(syncClaimRequest.getAddress())
					.collectionChannelId(syncClaimRequest.getCollectionChannelId())
					.collectorId(syncClaimRequest.getCollectorId())
					.content(syncClaimRequest.getContent())
					.crew(syncClaimRequest.getCrew())
					.folderCode(syncClaimRequest.getFolderCode())
					.gender(syncClaimRequest.getGender())
					.languageId(syncClaimRequest.getLanguageId())
					.objetId(syncClaimRequest.getObjetId())
					.phone(syncClaimRequest.getPhone())
					.productId(syncClaimRequest.getProductId())
					.receiptDateTime(syncClaimRequest.getReceiptDateTime())
					.servicePointId(syncClaimRequest.getServicePointId())
					.onlineUploadDateTime(LocalDateTime.now())
					.build();

			if (syncClaimRequest.getId() != null) {
				claimRequest.setId(syncClaimRequest.getId());

			}

			if (syncClaimRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
				if (syncClaimRequest.getId() != null) {
					// claimRequest.setId(syncClaimRequest.getId());
					if (syncClaimRequest.getCollectorId() != null) {
						saveRequest = SaveRequest
								.builder()
								.claimRequest(claimRequest)
								.files(syncClaimRequest.getFiles())
								.build();

						try {
							claimServiceImpl.saveTempClaimOffline(saveRequest, ClaimType.CLAIM);
						} catch (Exception e) {
							throw e;
						}
					}
				} else {
					if (syncClaimRequest.getCollectorId() != null) {
						saveRequest = SaveRequest
								.builder()
								.claimRequest(claimRequest)
								.files(syncClaimRequest.getFiles())
								.build();

						try {
							claimServiceImpl.saveTempClaimOffline(saveRequest, ClaimType.CLAIM);
						} catch (Exception e) {
							throw e;
						}
					}

				}

			} else {

				if (syncClaimRequest.getCollectorId() != null) {
					saveRequest = SaveRequest
							.builder()
							.claimRequest(claimRequest)
							.files(syncClaimRequest.getFiles())
							.build();
					try {
						claimServiceImpl.saveClaimOffline(saveRequest, ClaimType.CLAIM);
					} catch (Exception e) {
						throw e;

					}
				}
			}

			// System.out.println("syncClaimRequests");
			// System.out.println("code de l'obj"+syncClaimRequest.getCode());
			// try {
			// if (claimRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
			// claimServiceImpl.saveTempClaimOffline(saveRequest, ClaimType.CLAIM);
			// } else {
			// claimServiceImpl.saveClaimOffline(saveRequest, ClaimType.CLAIM);
			// }

			// // if (syncClaimRequest.getSolution() != null) {
			// // ProposedSolutionRequest proposedSolutionRequest = ProposedSolutionRequest
			// // .builder()
			// // .claimId(claim.getId())
			// // .commentaire(syncClaimRequest.getCommentaire())
			// // .solution(syncClaimRequest.getSolution())
			// // .treatorId(connectedUser.getId())

			// // .build();
			// // claim = claimServiceImpl.treatClaim(claim, connectedUser,
			// // proposedSolutionRequest);

			// // if (syncClaimRequest.getSatisfactionStatus() != null) {
			// // claim = claimServiceImpl.measureClaim(claim,
			// // claim.getSolutions().get(0), connectedUser,
			// // syncClaimRequest.getSatisfactionStatus());
			// // }

			// // }

			// } catch (Exception e) {
			// throw e;
			// // System.out.println("Error happened" + e.getMessage());

			// }
			// claims.add(claim);

		}

	}

	private void syncDenunList(List<SyncClaimRequest> syncClaimRequests) throws Exception {
		DenunRequest claimRequest;
		SaveDenunRequest saveRequest;

		for (SyncClaimRequest syncClaimRequest : syncClaimRequests) {

			claimRequest = DenunRequest
					.builder()
					.status(syncClaimRequest.getStatus())
					.code(syncClaimRequest.getCode())
					.createdAt(syncClaimRequest.getCreatedAt())

					.collectionChannelId(syncClaimRequest.getCollectionChannelId())
					.collectorId(syncClaimRequest.getCollectorId())
					.content(syncClaimRequest.getContent())

					.languageId(syncClaimRequest.getLanguageId())
					.objetId(syncClaimRequest.getObjetId())

					.productId(syncClaimRequest.getProductId())
					.receiptDateTime(syncClaimRequest.getReceiptDateTime())
					.servicePointId(syncClaimRequest.getServicePointId())
					.onlineUploadDateTime(LocalDateTime.now())
					.build();
			if (syncClaimRequest.getId() != null) {
				claimRequest.setId(syncClaimRequest.getId());
			}
			if (syncClaimRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
				if (syncClaimRequest.getId() != null) {
					// claimRequest.setId(syncClaimRequest.getId());
					if (syncClaimRequest.getCollectorId() != null) {
						saveRequest = SaveDenunRequest
								.builder()
								.claimRequest(claimRequest)
								.files(syncClaimRequest.getFiles())
								.build();

						try {
							claimServiceImpl.saveTempDenunOffline(saveRequest, ClaimType.DENUNCIACION);
						} catch (Exception e) {
							throw e;
						}
					}
				} else {
					if (syncClaimRequest.getCollectorId() != null) {
						saveRequest = SaveDenunRequest
								.builder()
								.claimRequest(claimRequest)
								.files(syncClaimRequest.getFiles())
								.build();

						try {
							claimServiceImpl.saveTempDenunOffline(saveRequest, ClaimType.DENUNCIACION);
						} catch (Exception e) {
							throw e;
						}
					}

				}
			} else {
				if (syncClaimRequest.getCollectorId() != null) {
					saveRequest = SaveDenunRequest
							.builder()
							.claimRequest(claimRequest)
							.files(syncClaimRequest.getFiles())
							.build();
					try {
						claimServiceImpl.saveDenunOffline(saveRequest, ClaimType.DENUNCIACION);
					} catch (Exception e) {
						throw e;

					}
				}
			}

			// if (syncClaimRequest.getId() != null) {
			// claimRequest.setId(syncClaimRequest.getId());
			// }
			// saveRequest = SaveDenunRequest
			// .builder()
			// .claimRequest(claimRequest)
			// .files(syncClaimRequest.getFiles())
			// .build();

			// try {
			// if (claimRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
			// claimServiceImpl.saveTempDenunOffline(saveRequest, ClaimType.DENUNCIACION);
			// } else {
			// claimServiceImpl.saveDenunOffline(saveRequest, ClaimType.DENUNCIACION);
			// }

			// // if (syncClaimRequest.getSolution() != null) {
			// // ProposedSolutionRequest proposedSolutionRequest = ProposedSolutionRequest
			// // .builder()
			// // .claimId(claim.getId())
			// // .commentaire(syncClaimRequest.getCommentaire())
			// // .solution(syncClaimRequest.getSolution())
			// // .treatorId(connectedUser.getId())

			// // .build();
			// // claim = claimServiceImpl.treatClaim(claim, connectedUser,
			// // proposedSolutionRequest);

			// // if (syncClaimRequest.getSatisfactionStatus() != null) {
			// // claim = claimServiceImpl.measureClaim(claim,
			// // claim.getSolutions().get(0), connectedUser,
			// // syncClaimRequest.getSatisfactionStatus());
			// // }

			// // }

			// } catch (Exception e) {
			// throw e;
			// // System.out.println("Error happened" + e.getMessage());
			// // return ResponseEntity.ok(apiResponseDto);
			// }

		}
	}

	private void syncSuggestionList(List<SyncSuggestionRequest> syncSuggestionRequests) throws Exception {
		SuggestionRequest suggestionRequest;
		// Suggestion suggestion = Suggestion.builder().build();

		// List<Suggestion> suggestions = new ArrayList<>();
		syncSuggestionRequests.removeIf(syncSugg -> syncSugg.getCollectionChannelId() == null
				&& syncSugg.getCollectorId() == null && syncSugg.getProductId() == null
				&& syncSugg.getLanguageId() == null && syncSugg.getServicePointId() == null);

		for (SyncSuggestionRequest syncSuggestionRequest : syncSuggestionRequests) {
			suggestionRequest = SuggestionRequest
					.builder()
					.status(syncSuggestionRequest.getStatus())
					.code(syncSuggestionRequest.getCode())
					.clientFirstAndLastName(syncSuggestionRequest.getClientFirstAndLastName())
					.address(syncSuggestionRequest.getAddress())
					.collectionChannelId(syncSuggestionRequest.getCollectionChannelId())
					.collectorId(syncSuggestionRequest.getCollectorId())
					.content(syncSuggestionRequest.getContent())
					.crew(syncSuggestionRequest.getCrew())
					.folderCode(syncSuggestionRequest.getFolderCode())
					.gender(syncSuggestionRequest.getGender())
					.languageId(syncSuggestionRequest.getLanguageId())
					.objetId(syncSuggestionRequest.getObjetId())
					.phone(syncSuggestionRequest.getPhone())
					.productId(syncSuggestionRequest.getProductId())
					.receiptDateTime(syncSuggestionRequest.getReceiptDateTime())
					.servicePointId(syncSuggestionRequest.getServicePointId())
					.createdAt(syncSuggestionRequest.getCreatedAt())
					.onlineUploadDateTime(LocalDateTime.now())
					.build();
			if (syncSuggestionRequest.getId() != null) {
				suggestionRequest.setId(syncSuggestionRequest.getId());
			}
			if (suggestionRequest.getStatus() == ClaimStatus.TEMP_SAVED) {
				if (syncSuggestionRequest.getId() != null) {
					// suggestionRequest.setId(syncSuggestionRequest.getId());
					if (syncSuggestionRequest.getCollectorId() != null) {
						SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
								.builder()
								.files(syncSuggestionRequest.getFiles())
								.suggestionRequest(suggestionRequest)
								.build();

						try {
							suggestionServiceImpl.saveSuggestionOffline(suggestionAddRequest,
									syncSuggestionRequest.getStatus());
						} catch (Exception e) {
							throw e;
						}
					}
				} else {
					if (syncSuggestionRequest.getCollectorId() != null) {
						SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
								.builder()
								.files(syncSuggestionRequest.getFiles())
								.suggestionRequest(suggestionRequest)
								.build();

						try {
							suggestionServiceImpl.saveSuggestionOffline(suggestionAddRequest,
									syncSuggestionRequest.getStatus());
						} catch (Exception e) {
							throw e;
						}
					}

				}

			} else {
				if (syncSuggestionRequest.getCollectorId() != null) {
					SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
							.builder()
							.files(syncSuggestionRequest.getFiles())
							.suggestionRequest(suggestionRequest)
							.build();

					try {
						suggestionServiceImpl.saveSuggestionOffline(suggestionAddRequest,
								syncSuggestionRequest.getStatus());
					} catch (Exception e) {
						throw e;
					}
				}
			}

			// try {
			// suggestionServiceImpl.saveSuggestionOffline(suggestionAddRequest,
			// syncSuggestionRequest.getStatus());
			// // TreatSuggestionRequest treatSuggestionRequest = TreatSuggestionRequest
			// // .builder()
			// // .accepted(syncSuggestionRequest.isAccepted())
			// // .commentaire(syncSuggestionRequest.getCommentaire())
			// // .treatorId(connectedUser.getId())
			// // .id(suggestion.getId())
			// // .build();
			// // suggestion = suggestionServiceImpl.treatSuggestion(suggestion,
			// connectedUser,
			// // treatSuggestionRequest);
			// } catch (Exception e) {
			// throw e;
			// // System.out.println("Error happened" + e.getMessage());
			// // apiResponseDto = ApiResponseDto
			// // .builder()
			// // .content(ErrorResponse.builder().title("Une erreur est survenue")
			// // .message("Impossible de sauvegarder la suggestion de "
			// // + syncSuggestionRequest
			// // .getClientFirstAndLastName()))
			// // .status(false)
			// // .build();
			// // return ResponseEntity.ok(apiResponseDto);
			// }
			// // suggestions.add(suggestion);
		}
	}
}
