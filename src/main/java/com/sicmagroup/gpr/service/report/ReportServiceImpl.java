package com.sicmagroup.gpr.service.report;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.dto.reports.RgbColor;
import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBarDataset;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineChart;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineDataset;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.dto.reports.tableTotal.ObjectTotal;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.ClaimSpecification;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.projection.ClaimPerCanalPerSpPjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderAndAgencePrjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderPjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelAndAgenceProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerServicePointProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerStatusSatisfactionProjection;
import com.sicmagroup.gpr.repository.projection.ObjectPerCanalProjection;
import com.sicmagroup.gpr.repository.projection.ObjectPerObjProjction;
import com.sicmagroup.gpr.repository.projection.ObjectTotalPerStatusProjection;
import com.sicmagroup.gpr.repository.projection.SuggestPerServicePointProjection;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerCanalAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerServicePointPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerCanalPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerObjPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerYear;
import com.sicmagroup.gpr.repository.projection.custom.ObjectTotalPerStatusPro;
import com.sicmagroup.gpr.repository.projection.custom.SuggesPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.SuggestTotalPerStatusPro;
import com.sicmagroup.gpr.utils.Utils;

import io.jsonwebtoken.Claims;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final CollectionChannelRespository clRepository;
    private final ServicePointRepository spRepository;
    private final ObjetRepository oRepository;

    private static String CLAIM_BG_COLOR = "#FFCE56";
    private static String CLAIM_HOVER_COLOR = "#FFBA56";
    private static String DENUN_BG_COLOR = "#ff6384";
    private static String DENUN_HOVER_COLOR = "#ff5384";
    private static String SUGGEST_BG_COLOR = "#36A2EB";
    private static String SUGGEST_HOVER_COLOR = "#3691EB";

    private static String SERVICE_BG_POINT_COLOR = "#25AFBE";
    private static String SERVICE_HOVER_POINT_COLOR = "#258EBE";
    private static String SERVICE_BORDER_POINT_COLOR = "#259FBE";
    private static String SERVICE_HOVER_BORDER_POINT_COLOR = "#258FBE";

    private static String GENDER_MALE_BG_COLOR = "#3399FF";
    private static String GENDER_FEMALE_BG_COLOR = "#FF99CC";
    private static String GENDER_NON_DEFINI_BG_COLOR = "#CFD8DC";

    private static String GRAVE_BG_COLOR = "#FF0000";
    private static String MOYEN_BG_COLOR = "#F88F24";
    private static String MINEUR_BG_COLOR = "#90FF90";

    private static String SATISFIED_BG_COLOR = "#66FF00";
    private static String UNSATISFIED_BG_COLOR = "#FF595F";
    private static String PARTIAL_SATISFIED_BG_COLOR = "#C0900F";

    private static String UNRESPECTED_BG_COLOR = "#FF9933";

    @Override
    public PieChartDto repartitionClaimDenunSuggest(@Nullable FilterRequest request) {
        Long totalClaim = null;
        Long totalDenun = null;
        Long totalSuggest = (long) 0;
        if (request != null) {
            totalClaim = claimRepository.countClaimByCriterias(request, ClaimType.CLAIM);
            totalDenun = claimRepository.countClaimByCriterias(request, ClaimType.DENUNCIACION);
            if (request.getObjets() != null && request.getObjets().isEmpty()) {
                totalSuggest = suggestionRepository.countSuggestByCriterias(request);
            }
        } else {
            totalClaim = claimRepository.countByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
            totalDenun = claimRepository.countByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);
            totalSuggest = suggestionRepository.countByStatusNot(ClaimStatus.TEMP_SAVED);
        }

        Long total = totalClaim + totalDenun + totalSuggest;

        PieChartDto pieChartDto = PieChartDto
                .builder()
                .labels(Arrays.asList("Réclamations", "Dénonciations", "Suggestions"))
                .datas(Arrays.asList(Utils.parseDouble(Utils.percentCalculator(totalClaim, total)),
                        Utils.parseDouble(Utils.percentCalculator(totalDenun, total)),
                        Utils.parseDouble(Utils.percentCalculator(totalSuggest, total))))
                .backgroundColors(Arrays.asList(CLAIM_BG_COLOR, DENUN_BG_COLOR, SUGGEST_BG_COLOR))
                .hoverColors(Arrays.asList(CLAIM_HOVER_COLOR, DENUN_HOVER_COLOR, SUGGEST_HOVER_COLOR))
                .build();

        return pieChartDto;
    }

    @Override
    public BandChart numberClaimPerServicePoint(@Nullable FilterRequest request) {

        List<ServicePoint> allSp = spRepository.findAll();
        boolean isFind = false;
        BandChart bandChart = BandChart
                .builder()
                .labels(new ArrayList<>())
                .backgroundColors(new ArrayList<>())
                .borderColors(new ArrayList<>())
                .datas(new ArrayList<>())
                .build();
        if (request != null) {
            List<ClaimPerServicePointPro> allResult2 = claimRepository.countClaimByCriteriaAndByServicePoint(request,
                    ClaimType.CLAIM);

            for (ClaimPerServicePointPro projection : allResult2) {
                bandChart.getLabels().add(projection.getLibelle());
                bandChart.getDatas().add(projection.getTotal().doubleValue());
            }

        } else {
            List<ClaimPerServicePointProjection> allResult = claimRepository
                    .countClaimPerServicePoint(ClaimType.CLAIM);

            for (ServicePoint sp : allSp) {
                bandChart.getLabels().add(sp.getLibelle());
                for (ClaimPerServicePointProjection projection : allResult) {
                    if (projection.getServicePointId().equals(sp.getId())) {
                        bandChart.getDatas().add(projection.getTotal().doubleValue());
                        isFind = true;
                    }
                }
                if (!isFind) {
                    bandChart.getDatas().add((double) 0);
                }
                isFind = false;
            }

        }

        bandChart.getBackgroundColors().add(SERVICE_BG_POINT_COLOR);
        bandChart.getBorderColors().add(SERVICE_BORDER_POINT_COLOR);

        bandChart.setBorderWidth("1");

        return bandChart;

    }

    @Override
    public BandChart numberDenunPerServicePoint(@Nullable FilterRequest request) {
        List<ServicePoint> allSp = spRepository.findAll();
        boolean isFind = false;
        BandChart bandChart = BandChart
                .builder()
                .labels(new ArrayList<>())
                .backgroundColors(new ArrayList<>())
                .borderColors(new ArrayList<>())
                .datas(new ArrayList<>())
                .build();
        if (request != null) {
            List<ClaimPerServicePointPro> allResult2 = claimRepository.countClaimByCriteriaAndByServicePoint(request,
                    ClaimType.DENUNCIACION);

            for (ClaimPerServicePointPro projection : allResult2) {
                bandChart.getLabels().add(projection.getLibelle());

                bandChart.getDatas().add(projection.getTotal().doubleValue());

            }

        } else {
            List<ClaimPerServicePointProjection> allResult = claimRepository
                    .countClaimPerServicePoint(ClaimType.DENUNCIACION);

            for (ServicePoint sp : allSp) {
                bandChart.getLabels().add(sp.getLibelle());
                for (ClaimPerServicePointProjection projection : allResult) {
                    if (projection.getServicePointId().equals(sp.getId())) {
                        bandChart.getDatas().add(projection.getTotal().doubleValue());
                        isFind = true;
                    }
                }
                if (!isFind) {
                    bandChart.getDatas().add((double) 0);
                }
                isFind = false;
            }

        }

        bandChart.getBackgroundColors().add(SERVICE_BG_POINT_COLOR);
        bandChart.getBorderColors().add(SERVICE_BORDER_POINT_COLOR);

        bandChart.setBorderWidth("1");

        return bandChart;
    }

    @Override
    public BandChart numberSuggestPerServicePoint(@Nullable FilterRequest request) {

        List<ServicePoint> allSp = spRepository.findAll();
        BandChart bandChart = BandChart
                .builder()
                .labels(new ArrayList<>())
                .backgroundColors(new ArrayList<>())
                .borderColors(new ArrayList<>())
                .datas(new ArrayList<>())
                .build();
        boolean isFind = false;
        if (request != null) {
            List<ClaimPerServicePointPro> allResult = suggestionRepository
                    .countSuggestByCriteriaAndServicePoint(request);

            for (ClaimPerServicePointPro projection : allResult) {
                bandChart.getLabels().add(projection.getLibelle());

                bandChart.getDatas().add(projection.getTotal().doubleValue());

            }

        } else {
            List<SuggestPerServicePointProjection> allResult = suggestionRepository.countSuggestPerServicePoint();
            for (ServicePoint sp : allSp) {
                bandChart.getLabels().add(sp.getLibelle());
                for (SuggestPerServicePointProjection projection : allResult) {
                    if (projection.getServiceIndexeId() != null && projection.getServiceIndexeId().equals(sp.getId())) {
                        bandChart.getDatas().add(projection.getTotal().doubleValue());
                        isFind = true;
                    }

                }
                if (!isFind) {
                    bandChart.getDatas().add((double) 0);
                }
                isFind = false;
            }

            for (SuggestPerServicePointProjection projection : allResult) {
                if (projection.getServiceIndexeId() == null || projection.getServiceIndexeId() == 0) {
                    bandChart.getDatas().add(projection.getTotal().doubleValue());
                    bandChart.getLabels().add(projection.getLibelle());
                }

            }
        }

        bandChart.getBackgroundColors().add(SERVICE_BG_POINT_COLOR);
        bandChart.getBorderColors().add(SERVICE_BORDER_POINT_COLOR);
        bandChart.setBorderWidth("1");

        return bandChart;
    }

    @Override
    public PieChartDto repartitionClaimPerServicePoint(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        // List<ClaimPerServicePointProjection> allResult = null;

        if (request != null) {
            List<ClaimPerServicePointPro> allResult = claimRepository.countClaimByCriteriaAndByServicePoint(request,
                    ClaimType.CLAIM);
            long total = 0;
            for (ClaimPerServicePointPro projection : allResult) {
                total += projection.getTotal();
            }
            for (ClaimPerServicePointPro projection : allResult) {
                pieChartDto.getLabels().add(projection.getLibelle());

                pieChartDto.getDatas()
                        .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        } else {
            List<ClaimPerServicePointProjection> allResult = claimRepository
                    .countClaimPerServicePoint(ClaimType.CLAIM);
            List<ServicePoint> allSp = spRepository.findAll();
            long total = claimRepository.countByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);

            boolean isFind = false;
            for (ServicePoint sp : allSp) {
                pieChartDto.getLabels().add(sp.getLibelle());
                for (ClaimPerServicePointProjection projection : allResult) {
                    if (sp.getId().equals(projection.getServicePointId())) {
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        isFind = true;
                    }
                }
                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allSp.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        }

        return pieChartDto;

    }

    @Override
    public PieChartDto repartitionDenunPerServicePoint(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        // List<ClaimPerServicePointProjection> allResult = null;

        if (request != null) {
            List<ClaimPerServicePointPro> allResult = claimRepository.countClaimByCriteriaAndByServicePoint(request,
                    ClaimType.DENUNCIACION);
            long total = 0;
            for (ClaimPerServicePointPro projection : allResult) {
                total += projection.getTotal();
            }
            for (ClaimPerServicePointPro projection : allResult) {
                pieChartDto.getLabels().add(projection.getLibelle());

                pieChartDto.getDatas()
                        .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        } else {
            List<ClaimPerServicePointProjection> allResult = claimRepository
                    .countClaimPerServicePoint(ClaimType.DENUNCIACION);
            List<ServicePoint> allSp = spRepository.findAll();
            long total = claimRepository.countByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);

            boolean isFind = false;
            for (ServicePoint sp : allSp) {
                pieChartDto.getLabels().add(sp.getLibelle());
                for (ClaimPerServicePointProjection projection : allResult) {
                    if (sp.getId().equals(projection.getServicePointId())) {
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        isFind = true;
                    }
                }
                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allSp.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionSuggestPerServicePoint(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();

        if (request != null) {
            List<ClaimPerServicePointPro> allResult = suggestionRepository
                    .countSuggestByCriteriaAndServicePoint(request);
            long totalClaim = 0;
            for (ClaimPerServicePointPro projection : allResult) {
                totalClaim += projection.getTotal();
            }
            for (ClaimPerServicePointPro projection : allResult) {
                pieChartDto.getLabels().add(projection.getLibelle());
                pieChartDto.getDatas()
                        .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), totalClaim)));
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<SuggestPerServicePointProjection> allResult = suggestionRepository.countSuggestPerServicePoint();
            long totalClaim = 0;
            List<ServicePoint> allSp = spRepository.findAll();
            boolean isFind = false;
            for (SuggestPerServicePointProjection projection : allResult) {
                totalClaim += projection.getTotal();
            }
            for (ServicePoint sp : allSp) {
                pieChartDto.getLabels().add(sp.getLibelle());
                for (SuggestPerServicePointProjection projection : allResult) {
                    if (sp.getId().equals(projection.getServiceIndexeId())) {
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), totalClaim)));
                        isFind = true;
                    }
                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }

            }

            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public ObjectTotal statusTotals(ClaimType type, @Nullable FilterRequest request) {
        ObjectTotal objectTotals = new ObjectTotal();
        objectTotals.setStatusAndValue(new HashMap<>());
        int total = 0;
        // reclamation
        if (request != null) {
            if (type == ClaimType.CLAIM) {
                List<ObjectTotalPerStatusPro> listClaimProjection = claimRepository
                        .countClaimByCriteriaAndStatus(request, type);
                for (ObjectTotalPerStatusPro projection : listClaimProjection) {
                    total += projection.getTotal();
                    objectTotals.getStatusAndValue().put(projection.getStatus().name(), projection.getTotal());
                }
                if (request.getEtats() != null) {
                    if (request.getEtats().contains(ClaimStatus.SAVED)
                            && !objectTotals.getStatusAndValue().containsKey("SAVED")) {
                        objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.AFFECTED)
                            && !objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                        objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.TO_APPROUVED)
                            && !objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                        objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.DESAPPROUVED)
                            && !objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                        objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.TREAT)
                            && !objectTotals.getStatusAndValue().containsKey("TREAT")) {
                        objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.SATISFIED)
                            && !objectTotals.getStatusAndValue().containsKey("SATISFIED")) {
                        objectTotals.getStatusAndValue().put("SATISFIED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.UNSATISFIED)
                            && !objectTotals.getStatusAndValue().containsKey("UNSATISFIED")) {
                        objectTotals.getStatusAndValue().put("UNSATISFIED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.PARTIAL_SATISFIED)
                            && !objectTotals.getStatusAndValue().containsKey("PARTIAL_SATISFIED")) {
                        objectTotals.getStatusAndValue().put("PARTIAL_SATISFIED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.LITIGATION)
                            && !objectTotals.getStatusAndValue().containsKey("LITIGATION")) {
                        objectTotals.getStatusAndValue().put("LITIGATION", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.CLASSED)
                            && !objectTotals.getStatusAndValue().containsKey("CLASSED")) {
                        objectTotals.getStatusAndValue().put("CLASSED", (long) 0);
                    }
                } else {
                    if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                        objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                        objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                        objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                        objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("TREAT")) {
                        objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("SATISFIED")) {
                        objectTotals.getStatusAndValue().put("SATISFIED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("UNSATISFIED")) {
                        objectTotals.getStatusAndValue().put("UNSATISFIED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("PARTIAL_SATISFIED")) {
                        objectTotals.getStatusAndValue().put("PARTIAL_SATISFIED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("LITIGATION")) {
                        objectTotals.getStatusAndValue().put("LITIGATION", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("CLASSED")) {
                        objectTotals.getStatusAndValue().put("CLASSED", (long) 0);
                    }
                }

            } else if (type == ClaimType.DENUNCIACION) {
                List<ObjectTotalPerStatusPro> listClaimProjection = claimRepository
                        .countClaimByCriteriaAndStatus(request, type);
                for (ObjectTotalPerStatusPro projection : listClaimProjection) {
                    total += projection.getTotal();
                    objectTotals.getStatusAndValue().put(projection.getStatus().name(), projection.getTotal());
                }
                if (request.getEtats() != null) {
                    if (request.getEtats().contains(ClaimStatus.SAVED)
                            && !objectTotals.getStatusAndValue().containsKey("SAVED")) {
                        objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.AFFECTED)
                            && !objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                        objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.TO_APPROUVED)
                            && !objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                        objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.DESAPPROUVED)
                            && !objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                        objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                    }
                    if (request.getEtats().contains(ClaimStatus.TREAT)
                            && !objectTotals.getStatusAndValue().containsKey("TREAT")) {
                        objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                    }
                } else {
                    if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                        objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                        objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                        objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                        objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                    }
                    if (!objectTotals.getStatusAndValue().containsKey("TREAT")) {
                        objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                    }
                }

            } else {
                if (request.getObjets() != null && request.getObjets().isEmpty()) {
                    List<SuggestTotalPerStatusPro> listClaimProjection = suggestionRepository
                            .countSuggestByCriteriaAndStatus(request);
                    for (SuggestTotalPerStatusPro projection : listClaimProjection) {
                        total += projection.getTotal();
                        if (projection.getStatus() == ClaimStatus.TREAT) {
                            if (projection.isAccepted()) {
                                objectTotals.getStatusAndValue().put("ACCEPTED", projection.getTotal());
                            } else {
                                objectTotals.getStatusAndValue().put("UNACCEPTED", projection.getTotal());
                            }
                        } else {
                            objectTotals.getStatusAndValue().put("SAVED", projection.getTotal());
                        }
                    }
                    if (request.getEtats() != null) {
                        if (request.getEtats().contains(ClaimStatus.SAVED)
                                && !objectTotals.getStatusAndValue().containsKey("SAVED")) {
                            objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                        }
                        if (!objectTotals.getStatusAndValue().containsKey("ACCEPTED")) {
                            objectTotals.getStatusAndValue().put("ACCEPTED", (long) 0);
                        }
                        if (!objectTotals.getStatusAndValue().containsKey("UNACCEPTED")) {
                            objectTotals.getStatusAndValue().put("UNACCEPTED", (long) 0);
                        }
                    } else {
                        if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                            objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                        }
                        if (!objectTotals.getStatusAndValue().containsKey("ACCEPTED")) {
                            objectTotals.getStatusAndValue().put("ACCEPTED", (long) 0);
                        }
                        if (!objectTotals.getStatusAndValue().containsKey("UNACCEPTED")) {
                            objectTotals.getStatusAndValue().put("UNACCEPTED", (long) 0);
                        }
                    }
                } else {
                    objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                    objectTotals.getStatusAndValue().put("ACCEPTED", (long) 0);
                    objectTotals.getStatusAndValue().put("UNACCEPTED", (long) 0);
                }

            }

        } else {
            if (type == ClaimType.CLAIM) {
                List<ObjectTotalPerStatusProjection> listClaimProjection = claimRepository.countClaimPerStatus(type);
                for (ObjectTotalPerStatusProjection projection : listClaimProjection) {
                    total += projection.getTotal();
                    objectTotals.getStatusAndValue().put(projection.getStatus(), projection.getTotal());
                }

                if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                    objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                    objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                    objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                    objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("TREAT")) {
                    objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("SATISFIED")) {
                    objectTotals.getStatusAndValue().put("SATISFIED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("UNSATISFIED")) {
                    objectTotals.getStatusAndValue().put("UNSATISFIED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("PARTIAL_SATISFIED")) {
                    objectTotals.getStatusAndValue().put("PARTIAL_SATISFIED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("LITIGATION")) {
                    objectTotals.getStatusAndValue().put("LITIGATION", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("CLASSED")) {
                    objectTotals.getStatusAndValue().put("CLASSED", (long) 0);
                }
            } else if (type == ClaimType.DENUNCIACION) {
                List<ObjectTotalPerStatusProjection> listClaimProjection = claimRepository.countClaimPerStatus(type);
                for (ObjectTotalPerStatusProjection projection : listClaimProjection) {
                    total += projection.getTotal();
                    objectTotals.getStatusAndValue().put(projection.getStatus(), projection.getTotal());
                }

                if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                    objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("AFFECTED")) {
                    objectTotals.getStatusAndValue().put("AFFECTED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("TO_APPROUVED")) {
                    objectTotals.getStatusAndValue().put("TO_APPROUVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("DESAPPROUVED")) {
                    objectTotals.getStatusAndValue().put("DESAPPROUVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("TREAT")) {
                    objectTotals.getStatusAndValue().put("TREAT", (long) 0);
                }
            } else {
                List<ObjectTotalPerStatusProjection> listClaimProjection = suggestionRepository.countSuggestPerStatus();
                for (ObjectTotalPerStatusProjection projection : listClaimProjection) {
                    total += projection.getTotal();
                    if (projection.getStatus() == "TREAT") {
                        if (projection.getAccepted()) {
                            objectTotals.getStatusAndValue().put("ACCEPTED", projection.getTotal());
                        } else {
                            objectTotals.getStatusAndValue().put("UNACCEPTED", projection.getTotal());
                        }
                    } else {
                        objectTotals.getStatusAndValue().put("SAVED", projection.getTotal());
                    }
                }

                if (!objectTotals.getStatusAndValue().containsKey("SAVED")) {
                    objectTotals.getStatusAndValue().put("SAVED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("ACCEPTED")) {
                    objectTotals.getStatusAndValue().put("ACCEPTED", (long) 0);
                }
                if (!objectTotals.getStatusAndValue().containsKey("UNACCEPTED")) {
                    objectTotals.getStatusAndValue().put("UNACCEPTED", (long) 0);
                }
            }

        }
        objectTotals.setTotal(total);
        return objectTotals;
    }

    @Override
    public PieChartDto repartitionClaimByCanal(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;

        if (request != null) {
            List<ObjectPerCanalPro> allResult = claimRepository.countClaimByCriteriaAndCanal(request, ClaimType.CLAIM);
            // System.out.println(allResult);
            for (ObjectPerCanalPro objectPerCanalProjection : allResult) {
                total += objectPerCanalProjection.getTotal();
            }

            for (ObjectPerCanalPro objectPerCanalProjection : allResult) {
                pieChartDto.getLabels().add(objectPerCanalProjection.getLibelle());
                pieChartDto.getDatas()
                        .add(Utils
                                .parseDouble(
                                        Utils.percentCalculator(objectPerCanalProjection.getTotal(), total)));

            }

            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        } else {
            List<ObjectPerCanalProjection> allResult = claimRepository.countClaimPerCanal(ClaimType.CLAIM);
            List<CollectionChannel> allCl = clRepository.findAll();
            boolean isFind = false;

            for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                total += objectPerCanalProjection.getTotal();
            }

            for (CollectionChannel collectionChannel : allCl) {
                pieChartDto.getLabels().add(collectionChannel.getLibelle());
                for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                    if (collectionChannel.getLibelle().equals(objectPerCanalProjection.getLibelle())) {
                        pieChartDto.getDatas()
                                .add(Utils
                                        .parseDouble(
                                                Utils.percentCalculator(objectPerCanalProjection.getTotal(), total)));
                        isFind = true;
                    }

                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }
            }

            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getDatas().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionDenunByCanal(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;

        if (request != null) {
            List<ObjectPerCanalPro> allResult = claimRepository.countClaimByCriteriaAndCanal(request,
                    ClaimType.DENUNCIACION);
            for (ObjectPerCanalPro objectPerCanalProjection : allResult) {
                total += objectPerCanalProjection.getTotal();
            }

            for (ObjectPerCanalPro objectPerCanalProjection : allResult) {
                pieChartDto.getLabels().add(objectPerCanalProjection.getLibelle());
                pieChartDto.getDatas()
                        .add(Utils
                                .parseDouble(
                                        Utils.percentCalculator(objectPerCanalProjection.getTotal(), total)));

            }

            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getLabels().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));

        } else {
            List<ObjectPerCanalProjection> allResult = claimRepository.countClaimPerCanal(ClaimType.DENUNCIACION);
            List<CollectionChannel> allCl = clRepository.findAll();
            boolean isFind = false;

            for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                total += objectPerCanalProjection.getTotal();
            }

            for (CollectionChannel collectionChannel : allCl) {
                pieChartDto.getLabels().add(collectionChannel.getLibelle());
                for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                    if (collectionChannel.getLibelle().equals(objectPerCanalProjection.getLibelle())) {
                        pieChartDto.getDatas()
                                .add(Utils
                                        .parseDouble(
                                                Utils.percentCalculator(objectPerCanalProjection.getTotal(), total)));
                        isFind = true;
                    }

                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }
            }

            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionSuggestByCanal(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;
        if (request != null) {
            List<ObjectPerCanalPro> allResult = suggestionRepository.countSuggestByCriteriaAndCanal(request);
            for (ObjectPerCanalPro objectPerCanalPro : allResult) {
                total += objectPerCanalPro.getTotal();
            }
            for (ObjectPerCanalPro objectPerCanalPro : allResult) {
                pieChartDto.getLabels().add(objectPerCanalPro.getLibelle());
                pieChartDto.getDatas()
                        .add(Utils
                                .parseDouble(Utils.percentCalculator(objectPerCanalPro.getTotal(), total)));
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<ObjectPerCanalProjection> allResult = suggestionRepository.countSuggestPerCanal();
            List<CollectionChannel> allCl = clRepository.findAll();
            boolean isFind = false;
            for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                total += objectPerCanalProjection.getTotal();
            }
            for (CollectionChannel collectionChannel : allCl) {
                pieChartDto.getLabels().add(collectionChannel.getLibelle());
                for (ObjectPerCanalProjection objectPerCanalProjection : allResult) {
                    if (collectionChannel.getLibelle().equals(objectPerCanalProjection.getLibelle())) {
                        pieChartDto.getDatas()
                                .add(Utils
                                        .parseDouble(
                                                Utils.percentCalculator(objectPerCanalProjection.getTotal(), total)));

                        isFind = true;
                    }

                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(allResult.size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionObjectByCanal(@Nullable FilterRequest request) {
        long total = 0;
        HashMap<Long, Long> libTotal = new HashMap<>();
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        if (request != null) {
            List<ObjectPerCanalPro> allResultC = claimRepository.countClaimByCriteriaAndCanal(request,
                    ClaimType.CLAIM);
            List<ObjectPerCanalPro> allResultD = claimRepository.countClaimByCriteriaAndCanal(request,
                    ClaimType.DENUNCIACION);
            if (request.getObjets() != null && request.getObjets().isEmpty()) {
                List<ObjectPerCanalPro> allResultS = suggestionRepository.countSuggestByCriteriaAndCanal(request);

                for (ObjectPerCanalPro projection : allResultS) {
                    total += projection.getTotal();
                    if (!pieChartDto.getIds().contains(projection.getId())) {
                        pieChartDto.getLabels().add(projection.getLibelle());
                        pieChartDto.getIds().add(projection.getId());
                        libTotal.put(projection.getId(), projection.getTotal());
                    }

                }
            }

            for (ObjectPerCanalPro projection : allResultC) {
                total += projection.getTotal();
                if (libTotal.containsKey(projection.getId())) {
                    libTotal.replace(projection.getId(), libTotal.get(projection.getId()) + projection.getTotal());
                }
                if (!pieChartDto.getIds().contains(projection.getId())) {
                    pieChartDto.getLabels().add(projection.getLibelle());
                    pieChartDto.getIds().add(projection.getId());
                    libTotal.put(projection.getId(), projection.getTotal());
                }
            }

            for (ObjectPerCanalPro projection : allResultD) {
                total += projection.getTotal();
                if (libTotal.containsKey(projection.getId())) {
                    libTotal.replace(projection.getId(), libTotal.get(projection.getId()) + projection.getTotal());
                }
                if (!pieChartDto.getIds().contains(projection.getId())) {
                    pieChartDto.getLabels().add(projection.getLibelle());
                    pieChartDto.getIds().add(projection.getId());
                    libTotal.put(projection.getId(), projection.getTotal());
                }
            }
            for (int i = 0; i < pieChartDto.getIds().size(); i++) {
                pieChartDto.getDatas()
                        .add(Utils.parseDouble(
                                Utils.percentCalculator(libTotal.get(pieChartDto.getIds().get(i)), total)));
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getLabels().size());

            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<ObjectPerCanalProjection> allResultC = claimRepository.countClaimPerCanal(ClaimType.CLAIM);
            List<ObjectPerCanalProjection> allResultD = claimRepository.countClaimPerCanal(ClaimType.DENUNCIACION);
            List<ObjectPerCanalProjection> allResultS = suggestionRepository.countSuggestPerCanal();

            for (ObjectPerCanalProjection projection : allResultS) {
                total += projection.getTotal();
                pieChartDto.getLabels().add(projection.getLibelle());
                pieChartDto.getIds().add(projection.getId());
                libTotal.put(projection.getId(), projection.getTotal());
            }
            for (ObjectPerCanalProjection projection : allResultC) {
                total += projection.getTotal();
                if (libTotal.containsKey(projection.getId())) {
                    libTotal.replace(projection.getId(), libTotal.get(projection.getId()) + projection.getTotal());
                }
                if (!pieChartDto.getIds().contains(projection.getId())) {
                    pieChartDto.getLabels().add(projection.getLibelle());
                    pieChartDto.getIds().add(projection.getId());
                    libTotal.put(projection.getId(), projection.getTotal());
                }
            }
            for (ObjectPerCanalProjection projection : allResultD) {
                total += projection.getTotal();
                if (libTotal.containsKey(projection.getId())) {
                    libTotal.replace(projection.getId(), libTotal.get(projection.getId()) + projection.getTotal());
                }
                if (!pieChartDto.getIds().contains(projection.getId())) {
                    pieChartDto.getLabels().add(projection.getLibelle());
                    pieChartDto.getIds().add(projection.getId());
                    libTotal.put(projection.getId(), projection.getTotal());
                }
            }

            for (int i = 0; i < pieChartDto.getIds().size(); i++) {
                pieChartDto.getDatas()
                        .add(Utils.parseDouble(
                                Utils.percentCalculator(libTotal.get(pieChartDto.getIds().get(i)), total)));
            }

            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getLabels().size());

            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public StackedBar numberClaimByCanalByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .labels(new ArrayList<>())
                .datasets(new ArrayList<>())
                .build();
        List<ServicePoint> allSPoints = spRepository.findAll();

        if (request != null) {
            List<ClaimPerCanalAndSpPro> allResult = claimRepository.countClaimByCriteriaAndCanalAndSp(request,
                    ClaimType.CLAIM);
            List<CollectionChannel> allCl = clRepository.findAll();
            List<Double> data = new ArrayList<>();
            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }

            //
            // for (ClaimPerCanalAndSpPro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getSpId())) {
            // stackedBar.getIds().add(projection.getSpId());
            // stackedBar.getLabels().add(projection.getSpLibelle());
            // data.add((double) 0);
            // }

            // }
            boolean canalExist = false;
            for (CollectionChannel collectionChannel : allCl) {
                for (ClaimPerCanalAndSpPro projection : allResult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        canalExist = true;
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }

                }
                if (canalExist) {
                    StackedBarDataset barDataset2 = StackedBarDataset
                            .builder()
                            .id(collectionChannel.getId())
                            .data(data)
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .label(collectionChannel.getLibelle())
                            .build();
                    stackedBar.getDatasets().add(barDataset2);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {

                        data.add((double) 0);

                    }
                    canalExist = false;
                }

            }

        } else {
            List<CollectionChannel> allCl = clRepository.findAll();
            List<ClaimPerCanalPerSpPjt> allresult = claimRepository.countClaimPerCanalAndAgence(ClaimType.CLAIM);
            List<Double> data = new ArrayList<>();
            //
            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }

            for (CollectionChannel collectionChannel : allCl) {

                for (ClaimPerCanalPerSpPjt projection : allresult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(collectionChannel.getId())
                        .label(collectionChannel.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
            }

        }

        return stackedBar;
    }

    @Override
    public StackedBar numberDenunByCanalByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .labels(new ArrayList<>())
                .datasets(new ArrayList<>())
                .build();
        List<ServicePoint> allSPoints = spRepository.findAll();
        if (request != null) {
            List<ClaimPerCanalAndSpPro> allResult = claimRepository.countClaimByCriteriaAndCanalAndSp(request,
                    ClaimType.DENUNCIACION);
            List<CollectionChannel> allCl = clRepository.findAll();
            List<Double> data = new ArrayList<>();
            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }
            //
            // for (ClaimPerCanalAndSpPro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getSpId())) {
            // stackedBar.getIds().add(projection.getSpId());
            // stackedBar.getLabels().add(projection.getSpLibelle());
            // data.add((double) 0);
            // }

            // }
            boolean canalExist = false;

            for (CollectionChannel collectionChannel : allCl) {
                for (ClaimPerCanalAndSpPro projection : allResult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        canalExist = true;
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }

                }
                if (canalExist) {
                    canalExist = false;
                    StackedBarDataset barDataset2 = StackedBarDataset
                            .builder()
                            .id(collectionChannel.getId())
                            .data(data)
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .label(collectionChannel.getLibelle())
                            .build();
                    stackedBar.getDatasets().add(barDataset2);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {

                        data.add((double) 0);

                    }
                }

            }

        } else {

            List<CollectionChannel> allCl = clRepository.findAll();
            List<ClaimPerCanalPerSpPjt> allresult = claimRepository.countClaimPerCanalAndAgence(ClaimType.DENUNCIACION);
            List<Double> data = new ArrayList<>();
            //
            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }

            for (CollectionChannel collectionChannel : allCl) {

                for (ClaimPerCanalPerSpPjt projection : allresult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(collectionChannel.getId())
                        .label(collectionChannel.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
            }

        }

        return stackedBar;
    }

    @Override
    public StackedBar numberSuggestByCanalByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .labels(new ArrayList<>())
                .datasets(new ArrayList<>())
                .build();
        List<ServicePoint> allSPoints = spRepository.findAll();
        if (request != null) {
            List<CollectionChannel> allCl = clRepository.findAll();
            List<ClaimPerCanalAndSpPro> allResult = suggestionRepository.countSuggestByCriteriaAndCanalAndSp(request);
            List<Double> data;
            data = new ArrayList<>();

            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }
            stackedBar.getIds().add((long) 0);
            stackedBar.getLabels().add("Non défini");
            data.add((double) 0);
            // for (ClaimPerCanalAndSpPro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getSpId())) {
            // stackedBar.getIds().add(projection.getSpId());
            // stackedBar.getLabels().add(projection.getSpLibelle());
            // data.add((double) 0);
            // }

            // }
            boolean canalExist = false;

            for (CollectionChannel collectionChannel : allCl) {
                for (ClaimPerCanalAndSpPro projection : allResult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        canalExist = true;
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }

                }
                if (canalExist) {
                    canalExist = false;
                    StackedBarDataset barDataset2 = StackedBarDataset
                            .builder()
                            .id(collectionChannel.getId())
                            .data(data)
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .label(collectionChannel.getLibelle())
                            .build();
                    stackedBar.getDatasets().add(barDataset2);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {

                        data.add((double) 0);

                    }
                    data.add((double) 0);
                }

            }
        } else {

            List<CollectionChannel> allCl = clRepository.findAll();
            List<ClaimPerCanalPerSpPjt> allresult = suggestionRepository.countSuggestPerCanalAndAgence();
            List<Double> data;
            data = new ArrayList<>();

            for (ServicePoint sPoint : allSPoints) {
                stackedBar.getIds().add(sPoint.getId());
                stackedBar.getLabels().add(sPoint.getLibelle());
                data.add((double) 0);
            }
            stackedBar.getIds().add((long) 0);
            stackedBar.getLabels().add("Non défini");
            data.add((double) 0);

            for (CollectionChannel collectionChannel : allCl) {

                for (ClaimPerCanalPerSpPjt projection : allresult) {
                    if (projection.getCanalId().equals(collectionChannel.getId())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(collectionChannel.getId())
                        .label(collectionChannel.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
                data.add((double) 0);
            }

        }

        return stackedBar;
    }

    @Override
    public StackedBar numberObjectByCanalByAgence(@Nullable FilterRequest request) {
        StackedBar claimStacked = this.numberClaimByCanalByAgence(request);
        StackedBar denunStacked = this.numberDenunByCanalByAgence(request);
        StackedBar suggestStacked = null;
        if (request != null && request.getObjets() != null && request.getObjets().isEmpty()) {
            suggestStacked = this.numberSuggestByCanalByAgence(request);
        } else if (request == null) {
            suggestStacked = this.numberSuggestByCanalByAgence(null);
        } else {
            suggestStacked = StackedBar
                    .builder()
                    .ids(new ArrayList<>())
                    .datasets(new ArrayList<>())
                    .labels(new ArrayList<>())
                    .build();
        }

        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        if (suggestStacked.getLabels().isEmpty()) {
            stackedBar = claimStacked;
        } else {
            stackedBar = suggestStacked;
        }

        if (!suggestStacked.getLabels().isEmpty()) {
            for (String label : claimStacked.getLabels()) {
                if (!stackedBar.getLabels().contains(label)) {
                    stackedBar.getLabels().add(label);
                    stackedBar.getIds().add(claimStacked.getIds().get(claimStacked.getLabels().indexOf(label)));
                    for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                        dataset.getData().add((double) 0);
                    }

                }
            }
        }

        for (String label : denunStacked.getLabels()) {
            if (!stackedBar.getLabels().contains(label)) {
                stackedBar.getLabels().add(label);
                stackedBar.getIds().add(denunStacked.getIds().get(denunStacked.getLabels().indexOf(label)));
                for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                    dataset.getData().add((double) 0);
                }

            }
        }
        boolean isFind = false;
        if (!suggestStacked.getLabels().isEmpty()) {
            for (StackedBarDataset datasetClaim : claimStacked.getDatasets()) {
                for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                    if (dataset.getLabel().equals(datasetClaim.getLabel())) {
                        isFind = true;
                        for (String label : claimStacked.getLabels()) {
                            double v = datasetClaim.getData().get(claimStacked.getLabels().indexOf(label));
                            dataset.getData().set(stackedBar.getLabels().indexOf(label),
                                    v + dataset.getData().get(stackedBar.getLabels().indexOf(label)));
                        }
                    }
                }
                if (!isFind) {
                    stackedBar.getDatasets().add(datasetClaim);

                } else {
                    isFind = false;
                }
            }
        }
        isFind = false;
        for (StackedBarDataset datasetDenun : denunStacked.getDatasets()) {
            for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                if (dataset.getLabel().equals(datasetDenun.getLabel())) {
                    isFind = true;
                    for (String label : denunStacked.getLabels()) {
                        double v = datasetDenun.getData().get(denunStacked.getLabels().indexOf(label));
                        dataset.getData().set(stackedBar.getLabels().indexOf(label),
                                v + dataset.getData().get(stackedBar.getLabels().indexOf(label)));
                    }
                }
            }
            if (!isFind) {
                stackedBar.getDatasets().add(datasetDenun);

            } else {
                isFind = false;
            }
        }

        return stackedBar;

    }

    @Override
    public PieChartDto repartitionClaimPerObjet(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;
        if (request != null) {
            List<ObjectPerCanalPro> allResult = claimRepository.countClaimByCriteriaAndObjet(request, ClaimType.CLAIM);
            for (ObjectPerCanalPro projection : allResult) {
                total += projection.getTotal();
            }
            List<Objet> allObjets = oRepository.findAll();
            for (Objet objet : allObjets) {
                pieChartDto.getIds().add(objet.getId());
                pieChartDto.getLabels().add(objet.getLibelle());
                pieChartDto.getDatas().add((double) 0);
            }

            for (ObjectPerCanalPro projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());

                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<ObjectPerCanalProjection> allResult = claimRepository.countClaimPerObjet(ClaimType.CLAIM);

            for (ObjectPerCanalProjection projection : allResult) {
                total += projection.getTotal();
            }
            List<Objet> allObjets = oRepository.findAll();
            for (Objet objet : allObjets) {
                pieChartDto.getIds().add(objet.getId());
                pieChartDto.getLabels().add(objet.getLibelle());
                pieChartDto.getDatas().add((double) 0);
            }

            for (ObjectPerCanalProjection projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());

                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionDenunPerObjet(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;
        if (request != null) {
            List<ObjectPerCanalPro> allResult = claimRepository.countClaimByCriteriaAndObjet(request,
                    ClaimType.DENUNCIACION);
            for (ObjectPerCanalPro projection : allResult) {
                total += projection.getTotal();
            }
            List<Objet> allObjets = oRepository.findAll();
            for (Objet objet : allObjets) {
                pieChartDto.getIds().add(objet.getId());
                pieChartDto.getLabels().add(objet.getLibelle());
                pieChartDto.getDatas().add((double) 0);
            }

            for (ObjectPerCanalPro projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());

                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<ObjectPerCanalProjection> allResult = claimRepository.countClaimPerObjet(ClaimType.DENUNCIACION);

            for (ObjectPerCanalProjection projection : allResult) {
                total += projection.getTotal();
            }
            List<Objet> allObjets = oRepository.findAll();
            for (Objet objet : allObjets) {
                pieChartDto.getIds().add(objet.getId());
                pieChartDto.getLabels().add(objet.getLibelle());
                pieChartDto.getDatas().add((double) 0);
            }

            for (ObjectPerCanalProjection projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());

                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));

            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }
        return pieChartDto;
    }

    @Override
    public StackedBar numberObjByObjetByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();

        if (request != null) {
            List<Objet> allObjets = oRepository.findAll();
            List<ObjectPerObjPro> claimList = claimRepository.countObjByCriteriaAndByObjetAndAgence(request);
            List<Double> data = new ArrayList<>();
            for (ObjectPerObjPro projection : claimList) {
                if (!stackedBar.getIds().contains(projection.getIdSp())) {
                    stackedBar.getIds().add(projection.getIdSp());
                    stackedBar.getLabels().add(projection.getLibelleSp());
                    data.add((double) 0);
                }

            }
            boolean objetExist = false;

            for (Objet objet : allObjets) {

                for (ObjectPerObjPro projection : claimList) {
                    if (projection.getIdObj() == objet.getId()) {
                        objetExist = true;
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                    }
                }
                if (objetExist) {
                    objetExist = false;
                    StackedBarDataset stackedBarDataset = StackedBarDataset
                            .builder()
                            .id(objet.getId())
                            .label(objet.getLibelle())
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .data(data)
                            .build();
                    stackedBar.getDatasets().add(stackedBarDataset);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {
                        data.add((double) 0);
                    }

                }

            }
        } else {
            List<Objet> allObjets = oRepository.findAll();
            List<ServicePoint> allSPoints = spRepository.findAll();
            List<Double> data = new ArrayList<>();
            for (ServicePoint servicePoint : allSPoints) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            List<ObjectPerObjProjction> claimList = claimRepository.countObjtPerObjetPerAgence();
            for (Objet objet : allObjets) {

                for (ObjectPerObjProjction projection : claimList) {
                    if (projection.getIdObj() == objet.getId()) {
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(objet.getId())
                        .label(objet.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
            }

        }

        return stackedBar;

    }

    @Override
    public PieChartDto repartitionObjPerObjet(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        if (request != null) {
            List<ObjectPerCanalPro> allResult = claimRepository.countObjectByCriteriaAndObjet(request);

            long total = 0;
            for (ObjectPerCanalPro projection : allResult) {
                total += projection.getTotal();
            }
            for (ObjectPerCanalPro projection : allResult) {
                if (!pieChartDto.getIds().contains(projection.getId())) {
                    pieChartDto.getIds().add(projection.getId());
                    pieChartDto.getLabels().add(projection.getLibelle());
                    pieChartDto.getDatas().add((double) 0);
                }

            }

            for (ObjectPerCanalPro projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());
                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        } else {
            List<ObjectPerCanalProjection> allResult = claimRepository.countObjPerObjet();

            long total = 0;
            for (ObjectPerCanalProjection projection : allResult) {
                total += projection.getTotal();
            }
            List<Objet> allObjets = oRepository.findAll();
            for (Objet objet : allObjets) {
                pieChartDto.getIds().add(objet.getId());
                pieChartDto.getLabels().add(objet.getLibelle());
                pieChartDto.getDatas().add((double) 0);
            }

            for (ObjectPerCanalProjection projection : allResult) {
                int position = pieChartDto.getLabels().indexOf(projection.getLibelle());
                pieChartDto.getDatas().set(position,
                        Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
            }
            List<RgbColor> bgColors = Utils.generateRandomColor(pieChartDto.getIds().size());
            pieChartDto.setBackgroundColors(bgColors.stream().map(t -> t.toBgString()).collect(Collectors.toList()));
            pieChartDto.setHoverColors(bgColors.stream().map(t -> t.toBorderString()).collect(Collectors.toList()));
        }

        return pieChartDto;
    }

    @Override
    public StackedBar numberClaimPerObjetByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<Double> data = new ArrayList<>();
        List<Objet> allObjets = oRepository.findAll();
        if (request != null) {
            List<ObjectPerObjPro> claimList = claimRepository.countClaimByCriteriaAndByObjetAndAgence(request,
                    ClaimType.CLAIM);
            for (ObjectPerObjPro projection : claimList) {
                if (!stackedBar.getIds().contains(projection.getIdSp())) {
                    stackedBar.getIds().add(projection.getIdSp());
                    stackedBar.getLabels().add(projection.getLibelleSp());
                    data.add((double) 0);
                }
            }
            boolean objetExist = false;
            for (Objet objet : allObjets) {
                for (ObjectPerObjPro projection : claimList) {
                    if (objet.getId().equals(projection.getIdObj())) {
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                        objetExist = true;
                    }
                }
                if (objetExist) {
                    objetExist = false;
                    StackedBarDataset stackedBarDataset = StackedBarDataset
                            .builder()
                            .id(objet.getId())
                            .label(objet.getLibelle())
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .data(data)
                            .build();
                    stackedBar.getDatasets().add(stackedBarDataset);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {
                        data.add((double) 0);
                    }

                }
            }

        } else {
            List<ServicePoint> allSPoints = spRepository.findAll();

            for (ServicePoint servicePoint : allSPoints) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            List<ObjectPerObjProjction> claimList = claimRepository.countClaimPerObjetPerAgence(ClaimType.CLAIM);
            for (Objet objet : allObjets) {

                for (ObjectPerObjProjction projection : claimList) {
                    if (projection.getIdObj() == objet.getId()) {
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(objet.getId())
                        .label(objet.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
            }
        }

        return stackedBar;
    }

    @Override
    public StackedBar numberDenunPerObjetByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<Double> data = new ArrayList<>();
        List<Objet> allObjets = oRepository.findAll();
        if (request != null) {
            List<ObjectPerObjPro> claimList = claimRepository.countClaimByCriteriaAndByObjetAndAgence(request,
                    ClaimType.DENUNCIACION);
            for (ObjectPerObjPro projection : claimList) {
                if (!stackedBar.getIds().contains(projection.getIdSp())) {
                    stackedBar.getIds().add(projection.getIdSp());
                    stackedBar.getLabels().add(projection.getLibelleSp());
                    data.add((double) 0);
                }
            }
            boolean objetExist = false;
            for (Objet objet : allObjets) {
                for (ObjectPerObjPro projection : claimList) {
                    if (objet.getId().equals(projection.getIdObj())) {
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                        objetExist = true;
                    }
                }
                if (objetExist) {
                    objetExist = false;
                    StackedBarDataset stackedBarDataset = StackedBarDataset
                            .builder()
                            .id(objet.getId())
                            .label(objet.getLibelle())
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            .data(data)
                            .build();
                    stackedBar.getDatasets().add(stackedBarDataset);
                    data = new ArrayList<>();
                    for (long id : stackedBar.getIds()) {
                        data.add((double) 0);
                    }

                }
            }

        } else {
            List<ServicePoint> allSPoints = spRepository.findAll();

            for (ServicePoint servicePoint : allSPoints) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            List<ObjectPerObjProjction> claimList = claimRepository.countClaimPerObjetPerAgence(ClaimType.DENUNCIACION);
            for (Objet objet : allObjets) {

                for (ObjectPerObjProjction projection : claimList) {
                    if (projection.getIdObj() == objet.getId()) {
                        data.set(stackedBar.getIds().indexOf(projection.getIdSp()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .id(objet.getId())
                        .label(objet.getLibelle())
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSPoints) {

                    data.add((double) 0);
                }
            }
        }

        return stackedBar;
    }

    @Override
    public PieChartDto repartitionClaimByGender(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        if (request != null) {
            List<ClaimPerGenderPro> allResult = claimRepository.countClaimByCriteriaAndByGender(request);
            long total = 0;
            for (ClaimPerGenderPro projection : allResult) {
                total += projection.getTotal();
            }

            for (ClaimPerGenderPro projection : allResult) {
                pieChartDto.getDatas().add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                if (projection.getGender().equals(Gender.NON_DEFINI)) {
                    pieChartDto.getLabels().add(projection.getGender().name().replace("_", " "));
                    pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
                } else if (projection.getGender().equals(Gender.FEMME)) {
                    pieChartDto.getLabels().add(projection.getGender().name());
                    pieChartDto.getBackgroundColors().add(GENDER_FEMALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_FEMALE_BG_COLOR);
                } else {
                    pieChartDto.getLabels().add(projection.getGender().name());
                    pieChartDto.getBackgroundColors().add(GENDER_MALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_MALE_BG_COLOR);
                }
            }
        } else {
            List<ClaimPerGenderPjt> allResult = claimRepository.countClaimPerGender(ClaimType.CLAIM);
            long total = 0;
            for (ClaimPerGenderPjt projection : allResult) {
                total += projection.getTotal();
            }

            for (ClaimPerGenderPjt projection : allResult) {
                pieChartDto.getDatas().add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                if (projection.getGender() == "NON_DEFINI") {
                    pieChartDto.getLabels().add(projection.getGender().replace("_", " "));
                    pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
                } else if (projection.getGender() == "FEMME") {
                    pieChartDto.getLabels().add(projection.getGender());
                    pieChartDto.getBackgroundColors().add(GENDER_FEMALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_FEMALE_BG_COLOR);
                } else {
                    pieChartDto.getLabels().add(projection.getGender());
                    pieChartDto.getBackgroundColors().add(GENDER_MALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_MALE_BG_COLOR);
                }
            }

        }

        return pieChartDto;

    }

    @Override
    public PieChartDto repartitionDenunByGender(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        List<ClaimPerGenderPjt> allResult = claimRepository.countClaimPerGender(ClaimType.DENUNCIACION);
        long total = 0;
        for (ClaimPerGenderPjt projection : allResult) {
            total += projection.getTotal();
        }

        for (ClaimPerGenderPjt projection : allResult) {
            if (projection.getGender() == "NON_DEFINI") {
                pieChartDto.getLabels().add(projection.getGender().replace("_", " "));
                pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
            } else if (projection.getGender() == "FEMME") {
                pieChartDto.getLabels().add(projection.getGender());
                pieChartDto.getBackgroundColors().add(GENDER_FEMALE_BG_COLOR);
                pieChartDto.getHoverColors().add(GENDER_FEMALE_BG_COLOR);
            } else {
                pieChartDto.getLabels().add(projection.getGender());
                pieChartDto.getBackgroundColors().add(GENDER_MALE_BG_COLOR);
                pieChartDto.getHoverColors().add(GENDER_MALE_BG_COLOR);
            }
            pieChartDto.getDatas().add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionSuggestByGender(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;
        if (request != null) {
            List<ClaimPerGenderPro> allResult = suggestionRepository.countSuggestByCriteriaAndGender(request);
            for (ClaimPerGenderPro claimPerGenderPro : allResult) {
                total += claimPerGenderPro.getTotal();
            }
            // System.out.println(allResult);
            for (ClaimPerGenderPro projection : allResult) {
                pieChartDto.getDatas().add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                if (projection.getGender() != null && projection.getGender().equals(Gender.NON_DEFINI)) {
                    pieChartDto.getLabels().add(projection.getGender().name().replace("_", " "));
                    pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
                } else if (projection.getGender() != null && projection.getGender().equals(Gender.FEMME)) {
                    pieChartDto.getLabels().add(projection.getGender().name());
                    pieChartDto.getBackgroundColors().add(GENDER_FEMALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_FEMALE_BG_COLOR);
                } else if (projection.getGender() != null && projection.getGender().equals(Gender.HOMME)) {
                    pieChartDto.getLabels().add(projection.getGender().name());
                    pieChartDto.getBackgroundColors().add(GENDER_MALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_MALE_BG_COLOR);
                } else {
                    pieChartDto.getLabels().add("NON DEFINI");
                    pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
                }
            }
        } else {
            List<ClaimPerGenderPjt> allResult = suggestionRepository.countSuggestPerGender();

            for (ClaimPerGenderPjt projection : allResult) {
                total += projection.getTotal();
            }

            for (ClaimPerGenderPjt projection : allResult) {

                pieChartDto.getDatas().add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                if (projection.getGender() == "NON_DEFINI") {
                    pieChartDto.getLabels().add(projection.getGender().replace("_", " "));
                    pieChartDto.getBackgroundColors().add(GENDER_NON_DEFINI_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_NON_DEFINI_BG_COLOR);
                } else if (projection.getGender() == "FEMME") {
                    pieChartDto.getLabels().add(projection.getGender());
                    pieChartDto.getBackgroundColors().add(GENDER_FEMALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_FEMALE_BG_COLOR);
                } else {
                    pieChartDto.getLabels().add(projection.getGender());
                    pieChartDto.getBackgroundColors().add(GENDER_MALE_BG_COLOR);
                    pieChartDto.getHoverColors().add(GENDER_MALE_BG_COLOR);
                }
            }
        }

        return pieChartDto;
    }

    @Override
    public StackedBar numberClaimByGenderByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<ServicePoint> allSp = spRepository.findAll();
        List<Double> data = new ArrayList<>();
        List<Gender> alGenders = Arrays.asList(Gender.FEMME, Gender.HOMME);
        if (request != null) {
            List<ClaimPerGenderAndAgencePro> allResult = claimRepository
                    .countClaimByCriteriaAndByGenderAndAgence(request);

            for (ClaimPerGenderAndAgencePro projection : allResult) {
                if (!stackedBar.getIds().contains(projection.getId())) {
                    stackedBar.getIds().add((Long) projection.getId());
                    stackedBar.getLabels().add((String) projection.getLibelle());
                    data.add((double) 0);
                }
            }
            boolean spIsFind = false;
            for (Gender gender : alGenders) {

                for (ClaimPerGenderAndAgencePro pro : allResult) {
                    if (pro.getGender().equals(gender)) {
                        data.set(stackedBar.getIds().indexOf(pro.getId()), pro.getTotal().doubleValue());
                        spIsFind = true;
                    }
                }
                if (spIsFind) {
                    spIsFind = false;
                    StackedBarDataset stackedBarDataset = StackedBarDataset
                            .builder()
                            .label(gender.name())
                            .data(data)
                            .build();
                    if (gender.name().equals("HOMME")) {
                        stackedBarDataset.setBackgroundColor(GENDER_MALE_BG_COLOR);
                    } else if (gender.name().equals("FEMME")) {
                        stackedBarDataset.setBackgroundColor(GENDER_FEMALE_BG_COLOR);
                    } else {
                        stackedBarDataset.setBackgroundColor(GENDER_NON_DEFINI_BG_COLOR);
                    }

                    stackedBar.getDatasets().add(stackedBarDataset);
                    data = new ArrayList<>();
                    for (int i = 0; i < stackedBar.getIds().size(); i++) {
                        data.add((double) 0);
                    }
                }
            }
        } else {
            List<ClaimPerGenderAndAgencePrjt> allresult = claimRepository.countClaimPerGenderAndAgence();

            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            // List<Gender> alGenders = Arrays.asList(Gender.FEMME, Gender.HOMME,
            // Gender.NON_DEFINI);

            for (Gender gender : alGenders) {
                for (ClaimPerGenderAndAgencePrjt projection : allresult) {
                    if (gender.name().equals(projection.getGender())) {
                        data.set(stackedBar.getIds().indexOf(projection.getId()), projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(gender.name())
                        .data(data)
                        .build();
                if (gender.name().equals("HOMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_MALE_BG_COLOR);
                } else if (gender.name().equals("FEMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_FEMALE_BG_COLOR);
                } else {
                    stackedBarDataset.setBackgroundColor(GENDER_NON_DEFINI_BG_COLOR);
                }

                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSp) {
                    data.add((double) 0);
                }
            }
        }

        return stackedBar;
    }

    @Override
    public StackedBar numberSuggestByGenderAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<ServicePoint> allSp = spRepository.findAll();
        List<Double> data = new ArrayList<>();
        List<Gender> alGenders = Arrays.asList(Gender.FEMME, Gender.HOMME, Gender.NON_DEFINI);

        if (request != null) {

            List<ClaimPerGenderAndAgencePro> allResult = suggestionRepository
                    .countSuggestByCriteriaAndGenderAndAgence(request);
            System.out.println("Suggestion result");
            System.out.println(allResult);
            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            data.add((double) 0);
            stackedBar.getIds().add((long) 0);
            stackedBar.getLabels().add("Non défini");
            // for (ClaimPerGenderAndAgencePro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getId())) {
            // stackedBar.getIds().add(projection.getId());
            // stackedBar.getLabels().add(projection.getLibelle());
            // data.add((double) 0);
            // }
            // }

            for (Gender gender : alGenders) {
                for (ClaimPerGenderAndAgencePro projection : allResult) {
                    if (projection.getGender() != null && projection.getGender().equals(gender)) {

                        if (projection.getId() != null) {
                            data.set(stackedBar.getIds().indexOf((projection.getId())),
                                    projection.getTotal().doubleValue());
                        }

                    } else if (projection.getGender() == null && gender.equals(Gender.NON_DEFINI)) {
                        // System.out.println("Suggestion result 2");
                        // System.out.println(projection.getId());
                        if (projection.getId() != null) {
                            data.set(stackedBar.getIds().indexOf((projection.getId())),
                                    projection.getTotal().doubleValue());
                        }
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(gender.name())
                        .data(data)
                        .build();
                if (gender.name().equals("HOMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_MALE_BG_COLOR);
                } else if (gender.name().equals("FEMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_FEMALE_BG_COLOR);
                } else {
                    stackedBarDataset.setLabel("NON DEFINI");
                    stackedBarDataset.setBackgroundColor(GENDER_NON_DEFINI_BG_COLOR);
                }

                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int index = 0; index < stackedBar.getIds().size(); index++) {
                    data.add((double) 0);
                }
                data.add((double) 0);
            }
        } else {
            List<ClaimPerGenderAndAgencePrjt> allresult = suggestionRepository.countSuggestPerGenderAndAgence();

            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            data.add((double) 0);
            stackedBar.getIds().add((long) 0);
            stackedBar.getLabels().add("Non défini");

            // List<Gender> alGenders = Arrays.asList(Gender.FEMME, Gender.HOMME,
            // Gender.NON_DEFINI);

            for (Gender gender : alGenders) {
                for (ClaimPerGenderAndAgencePrjt projection : allresult) {
                    if (gender.name().equals(projection.getGender())) {
                        int position = stackedBar.getIds().indexOf(projection.getId());
                        if (stackedBar.getIds().size() > position) {
                            data.set(position, projection.getTotal().doubleValue());
                        }
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(gender.name())
                        .data(data)
                        .build();
                if (gender.name().equals("HOMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_MALE_BG_COLOR);
                } else if (gender.name().equals("FEMME")) {
                    stackedBarDataset.setBackgroundColor(GENDER_FEMALE_BG_COLOR);
                } else {
                    stackedBarDataset.setLabel("NON DEFINI");
                    stackedBarDataset.setBackgroundColor(GENDER_NON_DEFINI_BG_COLOR);
                }

                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSp) {
                    data.add((double) 0);
                }
                data.add((double) 0);
            }
        }

        return stackedBar;
    }

    @Override
    public PieChartDto repartitionClaimByGravityLevel(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        List<GravityLevel> allLevels = Arrays.asList(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR);
        Long total = (long) 0;
        boolean isFind = false;
        if (request != null) {
            List<ClaimPerObjLevelPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevel(request,
                    ClaimType.CLAIM);
            System.out.println("allResult");
            System.out.println(allResult);
            for (ClaimPerObjLevelPro projection : allResult) {
                total += projection.getTotal();
            }
            for (GravityLevel gravityLevel : allLevels) {
                pieChartDto.getLabels().add(gravityLevel.name());
                for (ClaimPerObjLevelPro projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel)) {
                        // System.out.println("allResult2");
                        // System.out.println(projection.getObjNiveau());
                        // System.out.println(projection.getTotal());
                        // System.out.println(total);
                        // System.out.println(Utils.percentCalculator(projection.getTotal(), total));
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        switch (gravityLevel) {
                            case GRAVE:
                                pieChartDto.getBackgroundColors().add(GRAVE_BG_COLOR);
                                break;
                            case MINEUR:
                                pieChartDto.getBackgroundColors().add(MINEUR_BG_COLOR);
                                break;
                            case MOYEN:
                                pieChartDto.getBackgroundColors().add(MOYEN_BG_COLOR);
                                break;
                            default:
                                break;
                        }
                    }
                }

                // if (!isFind) {
                // pieChartDto.getDatas().add((double) 0);
                // }
                // isFind = false;
            }

        } else {
            List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.CLAIM);

            total = claimRepository.countByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);

            for (GravityLevel gravityLevel : allLevels) {
                pieChartDto.getLabels().add(gravityLevel.name());
                for (ClaimPerObjLevelProjection projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel.name())) {
                        isFind = true;
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        switch (gravityLevel) {
                            case GRAVE:
                                pieChartDto.getBackgroundColors().add(GRAVE_BG_COLOR);
                                break;
                            case MINEUR:
                                pieChartDto.getBackgroundColors().add(MINEUR_BG_COLOR);
                                break;
                            case MOYEN:
                                pieChartDto.getBackgroundColors().add(MOYEN_BG_COLOR);
                                break;
                            default:
                                break;
                        }

                    }
                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                }
                isFind = false;
            }
        }

        return pieChartDto;
    }

    @Override
    public PieChartDto repartitionDenunByGravityLevel(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        List<GravityLevel> allLevels = Arrays.asList(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR);
        Long total = (long) 0;
        boolean isFind = false;
        if (request != null) {
            List<ClaimPerObjLevelPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevel(request,
                    ClaimType.DENUNCIACION);
            for (ClaimPerObjLevelPro projection : allResult) {
                total += projection.getTotal();
            }
            for (GravityLevel gravityLevel : allLevels) {
                pieChartDto.getLabels().add(gravityLevel.name());
                for (ClaimPerObjLevelPro projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel)) {

                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        switch (gravityLevel) {
                            case GRAVE:
                                pieChartDto.getBackgroundColors().add(GRAVE_BG_COLOR);
                                break;
                            case MINEUR:
                                pieChartDto.getBackgroundColors().add(MINEUR_BG_COLOR);
                                break;
                            case MOYEN:
                                pieChartDto.getBackgroundColors().add(MOYEN_BG_COLOR);
                                break;
                            default:
                                break;
                        }
                    }
                }

            }

        } else {
            List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.DENUNCIACION);

            total = claimRepository.countByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);

            for (GravityLevel gravityLevel : allLevels) {
                pieChartDto.getLabels().add(gravityLevel.name());
                for (ClaimPerObjLevelProjection projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel.name())) {
                        isFind = true;
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        switch (gravityLevel) {
                            case GRAVE:
                                pieChartDto.getBackgroundColors().add(GRAVE_BG_COLOR);
                                break;
                            case MINEUR:
                                pieChartDto.getBackgroundColors().add(MINEUR_BG_COLOR);
                                break;
                            case MOYEN:
                                pieChartDto.getBackgroundColors().add(MOYEN_BG_COLOR);
                                break;
                            default:
                                break;
                        }
                    }
                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                }
                isFind = false;
            }
        }

        return pieChartDto;
    }

    @Override
    public StackedBar numberClaimByGravityByAngence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<GravityLevel> allLevels = Arrays.asList(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR);
        List<Double> data = new ArrayList<>();
        List<ServicePoint> allSp = spRepository.findAll();
        if (request != null) {
            List<ClaimPerObjLevelAndSpPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevelAndSp(request,
                    ClaimType.CLAIM);
            // System.out.println("allResult");
            // System.out.println(allResult);
            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }
            // for (ClaimPerObjLevelAndSpPro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getSpId())) {
            // stackedBar.getIds().add(projection.getSpId());
            // stackedBar.getLabels().add(projection.getSpLib());
            // data.add((double) 0);
            // }
            // }

            for (GravityLevel level : allLevels) {
                for (ClaimPerObjLevelAndSpPro projection : allResult) {
                    if (level.equals(projection.getObjNiveau())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(level.name())
                        .data(data)
                        .build();

                switch (level) {
                    case GRAVE:
                        stackedBarDataset.setBackgroundColor(GRAVE_BG_COLOR);
                        break;
                    case MINEUR:
                        stackedBarDataset.setBackgroundColor(MINEUR_BG_COLOR);
                        break;
                    case MOYEN:
                        stackedBarDataset.setBackgroundColor(MOYEN_BG_COLOR);
                        break;
                    default:
                        break;
                }
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int i = 0; i < stackedBar.getIds().size(); i++) {

                    data.add((double) 0);
                }
            }
        } else {

            List<ClaimPerObjLevelAndAgenceProjection> allresult = claimRepository
                    .countClaimPerObjLevelAndAgence(ClaimType.CLAIM);
            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            for (GravityLevel gravityLevel : allLevels) {
                for (ClaimPerObjLevelAndAgenceProjection projection : allresult) {
                    if (gravityLevel.name().equals(projection.getObjNiveau())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(gravityLevel.name())
                        .data(data)
                        .build();

                switch (gravityLevel) {
                    case GRAVE:
                        stackedBarDataset.setBackgroundColor(GRAVE_BG_COLOR);
                        break;
                    case MINEUR:
                        stackedBarDataset.setBackgroundColor(MINEUR_BG_COLOR);
                        break;
                    case MOYEN:
                        stackedBarDataset.setBackgroundColor(MOYEN_BG_COLOR);
                        break;

                }
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSp) {
                    data.add((double) 0);
                }

            }
        }

        return stackedBar;
    }

    @Override
    public StackedBar numberDenunByGravityByAgence(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<GravityLevel> allLevels = Arrays.asList(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR);
        List<Double> data = new ArrayList<>();
        List<ServicePoint> allSp = spRepository.findAll();
        if (request != null) {
            List<ClaimPerObjLevelAndSpPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevelAndSp(request,
                    ClaimType.DENUNCIACION);

            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }
            // for (ClaimPerObjLevelAndSpPro projection : allResult) {
            // if (!stackedBar.getIds().contains(projection.getSpId())) {
            // stackedBar.getIds().add(projection.getSpId());
            // stackedBar.getLabels().add(projection.getSpLib());
            // data.add((double) 0);
            // }
            // }

            for (GravityLevel level : allLevels) {
                for (ClaimPerObjLevelAndSpPro projection : allResult) {
                    if (level.equals(projection.getObjNiveau())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(level.name())
                        .data(data)
                        .build();

                switch (level) {
                    case GRAVE:
                        stackedBarDataset.setBackgroundColor(GRAVE_BG_COLOR);
                        break;
                    case MINEUR:
                        stackedBarDataset.setBackgroundColor(MINEUR_BG_COLOR);
                        break;
                    case MOYEN:
                        stackedBarDataset.setBackgroundColor(MOYEN_BG_COLOR);
                        break;

                }
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int i = 0; i < stackedBar.getIds().size(); i++) {

                    data.add((double) 0);
                }
            }
        } else {

            List<ClaimPerObjLevelAndAgenceProjection> allresult = claimRepository
                    .countClaimPerObjLevelAndAgence(ClaimType.DENUNCIACION);
            for (ServicePoint servicePoint : allSp) {
                stackedBar.getIds().add(servicePoint.getId());
                stackedBar.getLabels().add(servicePoint.getLibelle());
                data.add((double) 0);
            }

            for (GravityLevel gravityLevel : allLevels) {
                for (ClaimPerObjLevelAndAgenceProjection projection : allresult) {
                    if (gravityLevel.name().equals(projection.getObjNiveau())) {
                        data.set(stackedBar.getIds().indexOf(projection.getSpId()),
                                projection.getTotal().doubleValue());
                    }
                }
                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(gravityLevel.name())
                        .data(data)
                        .build();

                switch (gravityLevel) {
                    case GRAVE:
                        stackedBarDataset.setBackgroundColor(GRAVE_BG_COLOR);
                        break;
                    case MINEUR:
                        stackedBarDataset.setBackgroundColor(MINEUR_BG_COLOR);
                        break;
                    case MOYEN:
                        stackedBarDataset.setBackgroundColor(MOYEN_BG_COLOR);
                        break;
                    default:
                        break;
                }
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint servicePoint : allSp) {
                    data.add((double) 0);
                }

            }
        }

        return stackedBar;
    }

    @Override
    public PieChartDto repartitionClaimBySatisfaction(@Nullable FilterRequest request) {
        PieChartDto pieChartDto = PieChartDto
                .builder()
                .backgroundColors(new ArrayList<>())
                .hoverColors(new ArrayList<>())
                .labels(new ArrayList<>())
                .datas(new ArrayList<>())
                .ids(new ArrayList<>())
                .build();
        long total = 0;
        boolean isFind = false;
        List<ClaimStatus> allSatisfaction = Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED);
        if (request != null) {
            List<ObjectTotalPerStatusPro> allResult = claimRepository.countClaimByCriteriaAndSatisfaction(request);

            for (ObjectTotalPerStatusPro projection : allResult) {
                total += projection.getTotal();
            }

            for (ClaimStatus status : allSatisfaction) {

                for (ObjectTotalPerStatusPro projection : allResult) {
                    if (projection.getStatus().equals(status)) {
                        switch (status) {
                            case SATISFIED:
                                pieChartDto.getLabels().add("SATISFAIT");
                                pieChartDto.getBackgroundColors().add(SATISFIED_BG_COLOR);
                                break;
                            case UNSATISFIED:
                                pieChartDto.getLabels().add("NON SATISFAIT");
                                pieChartDto.getBackgroundColors().add(UNSATISFIED_BG_COLOR);
                                break;
                            case PARTIAL_SATISFIED:
                                pieChartDto.getLabels().add("PARTIELLEMENT SATISFAIT");
                                pieChartDto.getBackgroundColors().add(PARTIAL_SATISFIED_BG_COLOR);
                                break;
                            default:
                                break;
                        }
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        isFind = true;
                    }

                }
                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                } else {
                    isFind = false;
                }

            }
        } else {
            List<ClaimPerStatusSatisfactionProjection> allResult = claimRepository.countClaimPerSatisfaction();

            total = claimRepository.countByTypeAndStatusIn(ClaimType.CLAIM, allSatisfaction);

            for (ClaimStatus status : allSatisfaction) {

                for (ClaimPerStatusSatisfactionProjection projection : allResult) {
                    if (projection.getStatus().equals(status.name())) {
                        switch (status) {
                            case SATISFIED:
                                pieChartDto.getLabels().add("SATISFAIT");
                                pieChartDto.getBackgroundColors().add(SATISFIED_BG_COLOR);
                                break;
                            case UNSATISFIED:
                                pieChartDto.getLabels().add("NON SATISFAIT");
                                pieChartDto.getBackgroundColors().add(UNSATISFIED_BG_COLOR);
                                break;
                            case PARTIAL_SATISFIED:
                                pieChartDto.getLabels().add("PARTIELLEMENT SATISFAIT");
                                pieChartDto.getBackgroundColors().add(PARTIAL_SATISFIED_BG_COLOR);
                                break;
                            default:
                                break;
                        }
                        pieChartDto.getDatas()
                                .add(Utils.parseDouble(Utils.percentCalculator(projection.getTotal(), total)));
                        isFind = true;
                    }
                }

                if (!isFind) {
                    pieChartDto.getDatas().add((double) 0);
                }
                isFind = false;

            }

        }

        return pieChartDto;

    }

    @Override
    public StackedBar numberClaimTreatInDelaiOrNot(@Nullable FilterRequest request) {
        List<Claim> allClaims;

        if (request != null) {

            allClaims = claimRepository.findClaimByCriteriaAndTypeAndStatus(request,
                    Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED,
                            ClaimStatus.CLASSED));

        } else {
            allClaims = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM,
                    Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED,
                            ClaimStatus.CLASSED));
        }
        List<Objet> allObjets = oRepository.findAll();
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        List<Double> dataInDelai = new ArrayList<>();
        List<Double> dataNotInDelai = new ArrayList<>();
        for (Objet objet : allObjets) {
            stackedBar.getIds().add(objet.getId());
            stackedBar.getLabels().add(objet.getLibelle());
            dataInDelai.add((double) 0);
            dataNotInDelai.add((double) 0);
        }
        LocalDateTime receiptDate;
        LocalDateTime measureDate;
        LocalDateTime supposedFinalTreatmentDate;
        long delaiObj;
        // for (int i = 0; i < 2 ; i++) {
        for (Claim claim : allClaims) {
            receiptDate = claim.getReceiptDateTime();
            measureDate = claim.getSolutions().get((claim.getSolutions().size() - 1)).getSatisfactionMeasure()
                    .getMeasureDateTime();

            delaiObj = claim.getObjet().getProcessingTime();
            supposedFinalTreatmentDate = receiptDate.plusDays(delaiObj);
            if (measureDate.isAfter(supposedFinalTreatmentDate)) { // il y a retard de traitement
                dataNotInDelai.set(stackedBar.getIds().indexOf(claim.getObjet().getId()),
                        dataNotInDelai.get(stackedBar.getIds().indexOf(claim.getObjet().getId())) + 1);
            } else {
                dataInDelai.set(stackedBar.getIds().indexOf(claim.getObjet().getId()),
                        dataInDelai.get(stackedBar.getIds().indexOf(claim.getObjet().getId())) + 1);
            }

        }
        // if()

        StackedBarDataset stackedBarDataset = StackedBarDataset
                .builder()
                .label("Délai respecté")
                .backgroundColor(SERVICE_BG_POINT_COLOR)
                // .borderColor(CLAIM_BG_COLOR)
                .data(dataInDelai)
                .build();
        stackedBar.getDatasets().add(stackedBarDataset);

        StackedBarDataset stackedBarDataset2 = StackedBarDataset
                .builder()
                .label("Délai non respecté")
                .backgroundColor(UNRESPECTED_BG_COLOR)
                .data(dataNotInDelai)
                .build();
        stackedBar.getDatasets().add(stackedBarDataset2);

        // }

        return stackedBar;
    }

    @Override
    public LineChart evolutionSatisfactionByYear(@Nullable FilterRequest request) {
        List<ClaimStatus> allSatisfaction = Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED);
        List<Claim> allClaims = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, allSatisfaction);
        LocalDate currentDate = LocalDate.now();
        List<String> last12Months = new ArrayList<>();
        List<String> last12MonthsEn = new ArrayList<>();
        List<Month> last12M = new ArrayList<>();
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.FRENCH);
        for (int i = 0; i < 12; i++) {
            last12M.add(currentDate.minus(Period.ofMonths(i)).getMonth());
            last12MonthsEn.add(currentDate.minus(Period.ofMonths(i)).getMonth().toString());
            last12Months.add(symbols.getMonths()[currentDate.minusMonths(i).getMonthValue() - 1]);
        }
        Collections.reverse(last12Months);
        Collections.reverse(last12M);
        Collections.reverse(last12MonthsEn);

        // List<String> months = Arrays.asList("Janvier", "Février", "Mars", "Avril",
        // "Mai", "Juin", "Juillet", "Août",
        // "Septembre", "Octobre", "Novembre", "Décembre");
        if (request != null) {
            allClaims = claimRepository.findClaimByCriteriaAndTypeAndStatus(request, allSatisfaction);
        } else {
            allClaims = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, allSatisfaction);
        }

        LineChart lineChart = LineChart
                .builder()
                .ids(new ArrayList<>())
                .labels(last12Months)
                .data(new ArrayList<>())
                .build();

        List<Double> dataSatisfied = new ArrayList<>();
        List<Double> dataUnSatisfied = new ArrayList<>();
        List<Double> dataPartial = new ArrayList<>();
        for (int i = 0; i < 12; i++) {

            dataSatisfied.add((double) 0);
            dataUnSatisfied.add((double) 0);
            dataPartial.add((double) 0);
        }
        int thisYear = LocalDateTime.now().getYear();
        LocalDateTime measureDate = null;
        for (Claim claim : allClaims) {
            measureDate = claim.getSolutions().get((claim.getSolutions().size() - 1))
                    .getSatisfactionMeasure()
                    .getMeasureDateTime();
            if (thisYear == measureDate.getYear()) {
                // int statisfactionMonthPosition = measureDate.getMonth().getValue();
                switch (claim.getStatus()) {
                    case SATISFIED:
                        dataSatisfied.set(last12MonthsEn.indexOf(measureDate.getMonth().toString()),
                                dataSatisfied.get(last12MonthsEn.indexOf(measureDate.getMonth().toString())) + 1);
                        break;
                    case UNSATISFIED:
                        dataUnSatisfied.set(last12MonthsEn.indexOf(measureDate.getMonth().toString()),
                                dataSatisfied.get(last12MonthsEn.indexOf(measureDate.getMonth().toString())) + 1);
                        break;

                    case PARTIAL_SATISFIED:
                        dataPartial.set(last12MonthsEn.indexOf(measureDate.getMonth().toString()),
                                dataSatisfied.get(last12MonthsEn.indexOf(measureDate.getMonth().toString())) + 1);
                        break;

                    default:
                        break;
                }
            }

        }
        LineDataset dataset1 = LineDataset
                .builder()
                .label("Satisfait")
                .data(dataSatisfied)
                .backgroundColor((SATISFIED_BG_COLOR))

                .borderColor(SATISFIED_BG_COLOR)
                .build();
        lineChart.getData().add(dataset1);

        LineDataset dataset2 = LineDataset
                .builder()
                .label("Non satisfait")
                .data(dataUnSatisfied)
                .backgroundColor(UNSATISFIED_BG_COLOR)
                .borderColor((UNSATISFIED_BG_COLOR))
                .build();

        lineChart.getData().add(dataset2);

        LineDataset dataset3 = LineDataset
                .builder()
                .label("Partiellement satisfait")
                .data(dataPartial)
                .backgroundColor((PARTIAL_SATISFIED_BG_COLOR))
                .borderColor((PARTIAL_SATISFIED_BG_COLOR))
                .build();

        lineChart.getData().add(dataset3);
        // period

        return lineChart;
    }

    @Override
    public LineChart evolutionClaimDenunSuggestbyYear(@Nullable FilterRequest request) {
        LocalDate currentDate = LocalDate.now();
        List<String> last12Months = new ArrayList<>();
        List<String> last12MonthsEn = new ArrayList<>();
        List<Month> last12M = new ArrayList<>();
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.FRENCH);
        for (int i = 0; i < 12; i++) {
            last12M.add(currentDate.minus(Period.ofMonths(i)).getMonth());
            last12MonthsEn.add(currentDate.minus(Period.ofMonths(i)).getMonth().toString());
            last12Months.add(symbols.getMonths()[currentDate.minusMonths(i).getMonthValue() - 1]);
        }
        Collections.reverse(last12Months);
        Collections.reverse(last12M);
        Collections.reverse(last12MonthsEn);

        int thisYear = currentDate.getYear();
        LineChart lineChart = LineChart
                .builder()
                .ids(new ArrayList<>())
                .labels(last12Months)
                .data(new ArrayList<>())
                .build();
        List<Double> dataC = new ArrayList<>();
        List<Double> dataD = new ArrayList<>();
        List<Double> dataS = new ArrayList<>();
        for (int i = 0; i < 12; i++) {

            dataC.add((double) 0);
            dataD.add((double) 0);
            dataS.add((double) 0);
        }
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (request != null) {
            if ((request.getReceiveStart() != null && request.getReceiveEnd() != null) || request.getYear() != null) {
                List<Claim> allC = claimRepository
                        .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.CLAIM);
                for (Claim claim : allC) {
                    dataC.set(last12MonthsEn.indexOf(claim.getReceiptDateTime().getMonth().toString()),
                            dataC.get(last12MonthsEn.indexOf(claim.getReceiptDateTime().getMonth().toString())) + 1);
                }
                List<Claim> allD = claimRepository.countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(
                        request, ClaimType.DENUNCIACION);
                for (Claim denun : allD) {
                    dataD.set(last12MonthsEn.indexOf(denun.getReceiptDateTime().getMonth().toString()),
                            dataD.get(last12MonthsEn.indexOf(denun.getReceiptDateTime().getMonth().toString())) + 1);
                }
                if (request.getObjets() != null && request.getObjets().isEmpty()) {
                    List<Suggestion> allS = suggestionRepository.countSuggestByCriteriaAndPeriode(request);
                    for (Suggestion suggestion : allS) {
                        dataS.set(last12MonthsEn.indexOf(suggestion.getReceiptDateTime().getMonth().toString()),
                                dataS.get(last12MonthsEn.indexOf(suggestion.getReceiptDateTime().getMonth().toString()))
                                        + 1);
                    }
                }
            } else {
                List<Claim> allC = claimRepository
                        .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.CLAIM);
                for (Claim claim : allC) {
                    if (claim.getReceiptDateTime().getYear() == (thisYear)) {
                        dataC.set(last12MonthsEn.indexOf(claim.getReceiptDateTime().getMonth().toString()),
                                dataC.get(
                                        last12MonthsEn.indexOf(claim.getReceiptDateTime().getMonth().toString())) + 1);
                    }

                }
                List<Claim> allD = claimRepository.countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(
                        request, ClaimType.DENUNCIACION);
                for (Claim denun : allD) {
                    if (denun.getReceiptDateTime().getYear() == (thisYear)) {
                        dataD.set(last12MonthsEn.indexOf(denun.getReceiptDateTime().getMonth().toString()),
                                dataD.get(last12MonthsEn.indexOf(denun.getReceiptDateTime().getMonth().toString()))
                                        + 1);
                    }
                }
                List<Suggestion> allS = suggestionRepository.countSuggestByCriteriaAndPeriode(request);
                if (request.getObjets() != null && request.getObjets().isEmpty()) {
                    for (Suggestion suggestion : allS) {
                        if (suggestion.getReceiptDateTime().getYear() == (thisYear)) {
                            dataS.set(last12MonthsEn.indexOf(suggestion.getReceiptDateTime().getMonth().toString()),
                                    dataS.get(last12MonthsEn
                                            .indexOf(suggestion.getReceiptDateTime().getMonth().toString()))
                                            + 1);
                        }
                    }
                }
            }
        } else {
            String monthStr = "";
            int i = 0;
            for (Month month : last12M) {
                monthStr = month.getValue() < 10 ? "0" + month.getValue() : month.getValue() + "";

                start = LocalDateTime.parse(thisYear + "-" + monthStr + "-01T00:00:00");
                end = LocalDateTime
                        .parse(thisYear + "-" + monthStr + "-" + month.length(currentDate.isLeapYear()) + "T23:59:59");

                dataC.set(i, (double) claimRepository.countByTypeAndStatusNotAndReceiptDateTimeBetween(ClaimType.CLAIM,
                        ClaimStatus.TEMP_SAVED, start, end));
                dataD.set(i, (double) claimRepository.countByTypeAndStatusNotAndReceiptDateTimeBetween(
                        ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED, start, end));
                dataS.set(i, (double) suggestionRepository
                        .countByStatusNotAndReceiptDateTimeBetween(ClaimStatus.TEMP_SAVED, start, end));
                i++;
            }
        }
        LineDataset lineDataset1 = LineDataset
                .builder()
                .backgroundColor(CLAIM_BG_COLOR)
                .borderColor(CLAIM_BG_COLOR)
                .data(dataC)
                .label("Réclamations")
                .build();
        lineChart.getData().add(lineDataset1);
        LineDataset lineDataset2 = LineDataset
                .builder()
                .backgroundColor((DENUN_BG_COLOR))
                .borderColor(DENUN_BG_COLOR)
                .data(dataD)
                .label("Dénonciations")
                .build();
        lineChart.getData().add(lineDataset2);
        LineDataset lineDataset3 = LineDataset
                .builder()
                .backgroundColor((SUGGEST_BG_COLOR))
                .borderColor(SUGGEST_BG_COLOR)
                .data(dataS)
                .label("Suggestions")
                .build();
        lineChart.getData().add(lineDataset3);

        return lineChart;
    }

    @Override
    public double tauxResolutionClaim(@Nullable FilterRequest request) {
        long totalTreatClaims = 0;
        long totalClaims = 0;
        if (request != null) {
            totalTreatClaims = claimRepository.countClaimByCriteriaAndTypeAndStatusIn(request, ClaimType.CLAIM,
                    Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                            ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED, ClaimStatus.LITIGATION))
                    .size();
            request.setEtats(null);
            totalClaims = claimRepository
                    .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.CLAIM).size();
        } else {
            totalTreatClaims = claimRepository.countByTypeAndStatusIn(ClaimType.CLAIM,
                    Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                            ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED, ClaimStatus.LITIGATION));
            totalClaims = claimRepository.countByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        }

        if (totalClaims != 0) {
            return Utils.parseDouble(((double) totalTreatClaims / totalClaims));
        } else {
            return (double) 0;
        }

    }

    @Override
    public double tauxResolutionDenun(@Nullable FilterRequest request) {
        long totalTreatClaims = 0;
        long totalClaims = 0;
        if (request != null) {
            totalTreatClaims = claimRepository.countClaimByCriteriaAndTypeAndStatusIn(request, ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.TREAT))
                    .size();
            request.setEtats(null);
            totalClaims = claimRepository
                    .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.DENUNCIACION)
                    .size();
        } else {
            totalTreatClaims = claimRepository.countByTypeAndStatusIn(ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.TREAT));
            totalClaims = claimRepository.countByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);
        }

        if (totalClaims != 0) {
            return Utils.parseDouble(((double) totalTreatClaims / totalClaims));
        } else {
            return (double) 0;
        }

    }

    @Override
    public StackedBar evolutionClaimBySpAndYear(@Nullable FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();

        List<ObjectPerYear> allResultat = claimRepository.countClaimByCriteriaAndYearAndSp(request, ClaimType.CLAIM);
        List<Double> data = new ArrayList<>();
        List<Integer> allYear = new ArrayList<>();
        List<ServicePoint> allSp = spRepository.findAll();
        if (request != null) {
            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }
            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
                // if (!stackedBar.getIds().contains(objY.getSpId())) {
                // stackedBar.getLabels().add(objY.getSpLib());
                // stackedBar.getIds().add(objY.getSpId());
                // data.add((double) 0);
                // }
            }
            for (Integer year : allYear) {
                for (ObjectPerYear objY : allResultat) {
                    if (year.equals(objY.getYear())) {
                        data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                    }
                }

                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(year + "")
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        // .borderColor(CLAIM_BG_COLOR)
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int i = 0; i < stackedBar.getIds().size(); i++) {

                    data.add((double) 0);
                }

            }

        } else {

            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }

            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
            }
            // System.out.println(allYear);
            for (Integer year : allYear) {
                for (ObjectPerYear objY : allResultat) {
                    if (year.equals(objY.getYear())) {
                        data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                    }
                }

                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(year + "")
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        // .borderColor(CLAIM_BG_COLOR)
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint sPoint : allSp) {

                    data.add((double) 0);
                }

            }

        }

        return stackedBar;
    }

    @Override
    public StackedBar evolutionDenunBySpAndYear(FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();

        List<ObjectPerYear> allResultat = claimRepository.countClaimByCriteriaAndYearAndSp(request,
                ClaimType.DENUNCIACION);
        List<Double> data = new ArrayList<>();
        List<Integer> allYear = new ArrayList<>();
        List<ServicePoint> allSp = spRepository.findAll();
        if (request != null) {
            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }
            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
                // if (!stackedBar.getIds().contains(objY.getSpId())) {
                // stackedBar.getLabels().add(objY.getSpLib());
                // stackedBar.getIds().add(objY.getSpId());
                // data.add((double) 0);
                // }
            }
            for (Integer year : allYear) {
                for (ObjectPerYear objY : allResultat) {
                    if (year.equals(objY.getYear())) {
                        data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                    }
                }

                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(year + "")
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        // .borderColor(CLAIM_BG_COLOR)
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int i = 0; i < stackedBar.getIds().size(); i++) {

                    data.add((double) 0);
                }

            }

        } else {

            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }

            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
            }
            if (!allYear.isEmpty()) {
                // System.out.println(allYear.size());
                for (Integer year : allYear) {
                    for (ObjectPerYear objY : allResultat) {
                        if (year.equals(objY.getYear())) {
                            data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                        }
                    }
                    // System.out.println(data);
                    StackedBarDataset stackedBarDataset = StackedBarDataset
                            .builder()
                            .label(year + "")
                            .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                            // .borderColor(CLAIM_BG_COLOR)
                            .data(data)
                            .build();
                    stackedBar.getDatasets().add(stackedBarDataset);
                    // System.out.println( stackedBar.getDatasets());
                    data = new ArrayList<>();
                    for (ServicePoint sPoint : allSp) {

                        data.add((double) 0);
                    }

                }
            }

        }

        return stackedBar;
    }

    @Override
    public double tauxResolutionSuggest(FilterRequest request) {
        int totalSuggestTreat = 0;

        if (request != null) {
            List<Suggestion> allResult = suggestionRepository.countSuggestByCriteriaAndPeriode(request);
            for (Suggestion suggestion : allResult) {
                if (suggestion.getStatus().equals(ClaimStatus.TREAT)) {
                    totalSuggestTreat++;
                }
            }

            if (allResult.size() != 0) {
                return Utils.parseDouble((double) totalSuggestTreat / allResult.size());
            } else {
                return (double) 0;
            }

        } else {
            List<Suggestion> allResult = suggestionRepository.findByStatusIn(Arrays.asList(ClaimStatus.TREAT));
            List<Suggestion> suggestNotTempSaved = suggestionRepository.findByStatusNot(ClaimStatus.TEMP_SAVED);

            if (suggestNotTempSaved.size() != 0) {
                return Utils.parseDouble((double) allResult.size() / suggestNotTempSaved.size());
            } else {
                return (double) 0;
            }

        }
    }

    @Override
    public double tauxResolutionObj(FilterRequest request) {
        long totalObj = 0;
        long totalTreatObj = 0;
        long totalTreatClaims = 0;
        long totalClaims = 0;
        long totalTreatDenun = 0;
        long totalDenuns = 0;
        int totalSuggestTreat = 0;
        if (request != null) {
            totalTreatDenun = claimRepository.countClaimByCriteriaAndTypeAndStatusIn(request, ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.TREAT))
                    .size();
            totalTreatClaims = claimRepository.countClaimByCriteriaAndTypeAndStatusIn(request, ClaimType.CLAIM,
                    Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                            ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED, ClaimStatus.LITIGATION))
                    .size();
            request.setEtats(null);
            totalDenuns = claimRepository
                    .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.DENUNCIACION)
                    .size();
            totalClaims = claimRepository
                    .countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(request, ClaimType.CLAIM).size();
            if (request.getObjets() != null && request.getObjets().isEmpty()) {
                List<Suggestion> allResult = suggestionRepository.countSuggestByCriteriaAndPeriode(request);
                for (Suggestion suggestion : allResult) {
                    if (suggestion.getStatus().equals(ClaimStatus.TREAT)) {
                        totalSuggestTreat++;
                    }
                }
                totalObj = totalClaims + totalDenuns + allResult.size();
            } else {
                totalObj = totalClaims + totalDenuns;
            }

            totalTreatObj = totalTreatDenun + totalTreatClaims + totalSuggestTreat;
        } else {
            totalTreatDenun = claimRepository.countByTypeAndStatusIn(ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.TREAT));
            totalDenuns = claimRepository.countByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);

            totalTreatClaims = claimRepository.countByTypeAndStatusIn(ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.TREAT));
            totalClaims = claimRepository.countByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);

            List<Suggestion> allTreatResult = suggestionRepository.findByStatusIn(Arrays.asList(ClaimStatus.TREAT));
            List<Suggestion> suggestNotTempSaved = suggestionRepository.findByStatusNot(ClaimStatus.TEMP_SAVED);

            totalObj = totalDenuns + totalClaims + suggestNotTempSaved.size();
            totalTreatObj = totalTreatDenun + totalTreatClaims + allTreatResult.size();
        }
        if (totalObj != 0) {
            return Utils.parseDouble((double) totalTreatObj / totalObj);
        } else {
            return 0;
        }

    }

    @Override
    public StackedBar evolutionSuggestBySpAndYear(FilterRequest request) {
        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();

        List<ObjectPerYear> allResultat = suggestionRepository.countSuggestByCriteriaAndYearAndSp(request);
        List<Double> data = new ArrayList<>();
        List<Integer> allYear = new ArrayList<>();
        List<ServicePoint> allSp = spRepository.findAll();
        if (request != null) {
            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }
            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
                // if (!stackedBar.getIds().contains(objY.getSpId())) {
                // stackedBar.getLabels().add(objY.getSpLib());
                // stackedBar.getIds().add(objY.getSpId());
                // data.add((double) 0);
                // }
            }
            for (Integer year : allYear) {
                for (ObjectPerYear objY : allResultat) {
                    if (year.equals(objY.getYear())) {
                        data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                    }
                }

                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(year + "")
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        // .borderColor(CLAIM_BG_COLOR)
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (int i = 0; i < stackedBar.getIds().size(); i++) {

                    data.add((double) 0);
                }

            }
        } else {

            for (ServicePoint sPoint : allSp) {
                stackedBar.getLabels().add(sPoint.getLibelle());
                stackedBar.getIds().add(sPoint.getId());
                data.add((double) 0);
            }

            for (ObjectPerYear objY : allResultat) {
                if (!allYear.contains(objY.getYear())) {
                    allYear.add(objY.getYear());
                }
            }

            for (Integer year : allYear) {
                for (ObjectPerYear objY : allResultat) {
                    if (year.equals(objY.getYear())) {
                        data.set(stackedBar.getIds().indexOf(objY.getSpId()), objY.getTotal().doubleValue());
                    }
                }

                StackedBarDataset stackedBarDataset = StackedBarDataset
                        .builder()
                        .label(year + "")
                        .backgroundColor(Utils.generateRandomColor(1).get(0).toBgString())
                        // .borderColor(CLAIM_BG_COLOR)
                        .data(data)
                        .build();
                stackedBar.getDatasets().add(stackedBarDataset);
                data = new ArrayList<>();
                for (ServicePoint sPoint : allSp) {

                    data.add((double) 0);
                }

            }
        }
        return stackedBar;
    }

    @Override
    public StackedBar evolutionObjBySpAndYear(FilterRequest request) {
        StackedBar stackedClaim = this.evolutionClaimBySpAndYear(request);
        StackedBar stackedDenun = this.evolutionDenunBySpAndYear(request);
        StackedBar stackedSuggest = null;
        if (request != null && request.getObjets() != null && request.getObjets().isEmpty()) {
            stackedSuggest = this.evolutionSuggestBySpAndYear(request);
        } else if (request == null) {
            stackedSuggest = this.evolutionSuggestBySpAndYear(null);
        } else {
            stackedSuggest = StackedBar
                    .builder()
                    .ids(new ArrayList<>())
                    .datasets(new ArrayList<>())
                    .labels(new ArrayList<>())
                    .build();
        }

        StackedBar stackedBar = StackedBar
                .builder()
                .ids(new ArrayList<>())
                .datasets(new ArrayList<>())
                .labels(new ArrayList<>())
                .build();
        if (stackedSuggest.getLabels().isEmpty()) {
            stackedBar = stackedClaim;
        } else {
            stackedBar = stackedSuggest;
        }
        if (!stackedSuggest.getLabels().isEmpty()) {
            for (String label : stackedClaim.getLabels()) {
                if (!stackedBar.getLabels().contains(label)) {
                    stackedBar.getLabels().add(label);
                    stackedBar.getIds().add(stackedClaim.getIds().get(stackedClaim.getLabels().indexOf(label)));
                    for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                        dataset.getData().add((double) 0);
                    }

                }
            }
        }

        for (String label : stackedDenun.getLabels()) {
            if (!stackedBar.getLabels().contains(label)) {
                stackedBar.getLabels().add(label);
                stackedBar.getIds().add(stackedDenun.getIds().get(stackedDenun.getLabels().indexOf(label)));
                for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                    dataset.getData().add((double) 0);
                }

            }
        }
        boolean isFind = false;
        if (!stackedSuggest.getLabels().isEmpty()) {
            for (StackedBarDataset datasetClaim : stackedClaim.getDatasets()) {
                for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                    if (dataset.getLabel().equals(datasetClaim.getLabel())) {
                        isFind = true;
                        for (String label : stackedClaim.getLabels()) {
                            double v = datasetClaim.getData().get(stackedClaim.getLabels().indexOf(label));
                            dataset.getData().set(stackedBar.getLabels().indexOf(label),
                                    v + dataset.getData().get(stackedBar.getLabels().indexOf(label)));
                        }
                    }
                }
                if (!isFind) {
                    stackedBar.getDatasets().add(datasetClaim);

                } else {
                    isFind = false;
                }
            }
        }
        isFind = false;
        for (StackedBarDataset datasetDenun : stackedDenun.getDatasets()) {
            for (StackedBarDataset dataset : stackedBar.getDatasets()) {
                if (dataset.getLabel().equals(datasetDenun.getLabel())) {
                    isFind = true;
                    for (String label : stackedDenun.getLabels()) {
                        double v = datasetDenun.getData().get(stackedDenun.getLabels().indexOf(label));
                        dataset.getData().set(stackedBar.getLabels().indexOf(label),
                                v + dataset.getData().get(stackedBar.getLabels().indexOf(label)));
                    }
                }
            }
            if (!isFind) {
                stackedBar.getDatasets().add(datasetDenun);

            } else {
                isFind = false;
            }
        }

        return stackedBar;
    }

    @Override
    public HashMap<String, Double> statisticsClaims(FilterRequest request) {
        // TODO Auto-generated method stub

        HashMap<String, Double> resultat = new HashMap<>();
        List<GravityLevel> allLevels = Arrays.asList(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR);
        List<ClaimStatus> filterStatus = new ArrayList<>();
        if (request != null) {
            // Nombre de réclamations à niveau de gravité faible / moyen / élevé
            // enregistrées
            // if( request.getEtats() != null && !request.getEtats().isEmpty()){
            // request.getEtats().removeIf(t -> !t.equals(ClaimStatus.TREAT) &&
            // !t.equals(ClaimStatus.CLASSED) && !t.equals(ClaimStatus.SATISFIED) &&
            // !t.equals(ClaimStatus.UNSATISFIED) &&
            // !t.equals(ClaimStatus.PARTIAL_SATISFIED) &&
            // !t.equals(ClaimStatus.LITIGATION));
            // } else {

            // }
            String key = "";
            List<ClaimPerObjLevelPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevel(request,
                    ClaimType.CLAIM);
            for (GravityLevel gravityLevel : allLevels) {
                for (ClaimPerObjLevelPro projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel)) {
                        switch (gravityLevel.name()) {
                            case "GRAVE":
                                key = "Nombre de réclamations à niveau de gravité élevé enregistrées";
                                break;
                            case "MOYEN":
                                key = "Nombre de réclamations à niveau de gravité moyen enregistrées";
                                break;
                            case "MINEUR":
                                key = "Nombre de réclamations à niveau de gravité mineur enregistrées";
                                break;

                        }
                        resultat.put(key, projection.getTotal().doubleValue());
                    }
                }
            }

        } else {
            // Nombre de réclamations à niveau de gravité faible / moyen / élevé
            // enregistrées
            List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.CLAIM);
            String key = "";
            for (GravityLevel gravityLevel : allLevels) {
                for (ClaimPerObjLevelProjection projection : allResult) {
                    if (projection.getObjNiveau().equals(gravityLevel.name())) {

                        switch (gravityLevel.name()) {
                            case "GRAVE":
                                key = "Nombre de réclamations à niveau de gravité élevé enregistrées";
                                break;
                            case "MOYEN":
                                key = "Nombre de réclamations à niveau de gravité moyen enregistrées";
                                break;
                            case "MINEUR":
                                key = "Nombre de réclamations à niveau de gravité mineur enregistrées";
                                break;

                        }
                        resultat.put(key, projection.getTotal().doubleValue());
                    }
                }
            }
        }

        throw new UnsupportedOperationException("Unimplemented method 'statisticsClaims'");
    }
}
