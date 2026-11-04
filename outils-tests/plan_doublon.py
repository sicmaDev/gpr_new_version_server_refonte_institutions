import sys, openpyxl
from copy import copy
f="Plan_de_test_GPR_Refonte_vierge.xlsx"
ligne, page, cas, donnees, resultat, etape = int(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], (sys.argv[6] if len(sys.argv)>6 else None)
wb=openpyxl.load_workbook(f); ws=wb.active
for c in range(1,30):
    s=ws.cell(90,c); d=ws.cell(ligne,c)
    if s.has_style: d._style=copy(s._style)
vals=[etape,"Refuser les doublons avec majuscules et espaces en trop","Administrateur (dev@sicmagroup.com)",cas,
 "Application locale sur la copie gpr_sicma_verif ; connecté en administrateur (dev@sicmagroup.com) ; page Configurations > "+page,"1",
 "Ajouter une entrée de test, puis le même nom en changeant les majuscules avec des espaces au début, à la fin et en double ; modifier une autre entrée vers ce nom ; supprimer l'entrée de test.",
 "Directive de l'utilisateur (04/11/2026) : corriger le même défaut que sur Modalités de dépôt sur les autres pages, de façon progressive, une page à la fois, vérifiée en direct.",
 "Oui",donnees,"Les deux variantes refusées (ajout et modification) avec message clair.","Variante acceptée comme nouvelle entrée.",resultat]
for c,v in enumerate(vals,1): ws.cell(ligne,c).value=v
ws.row_dimensions[ligne].height=ws.row_dimensions[90].height
wb.save(f); print("ligne", ligne, "ok")
