"""Optional static comparison figure. Requires reportlab; input is validated comparison.json."""
import argparse
import json
import math
from pathlib import Path
from reportlab.graphics.shapes import Drawing, String, Rect
from reportlab.graphics.charts.barcharts import VerticalBarChart
from reportlab.graphics import renderSVG
from reportlab.lib import colors

def main(campaign):
    directory=Path(__file__).resolve().parents[1]/'docs/performance/web-results'/f'comparison-{campaign}'
    values=json.loads((directory/'comparison.json').read_text(encoding='utf-8'))
    drawing=Drawing(820,340)
    drawing.add(String(30,317,'JCash dashboard: baseline and bounded connection pool',fontName='Helvetica-Bold',fontSize=14))
    drawing.add(String(30,299,'Median of three 60-second trials per load; 100 transactions returned per customer.',fontName='Helvetica',fontSize=10,fillColor=colors.HexColor('#555555')))
    palette=[colors.HexColor('#9BA4A1'),colors.HexColor('#1F6655')]
    for x,title,keys,unit in [(55,'Successful requests / second',('baseline_rps','pooled_rps'),'Higher is better'),(470,'p95 response time (ms)',('baseline_p95_ms','pooled_p95_ms'),'Lower is better')]:
        chart=VerticalBarChart()
        chart.x=x;chart.y=85;chart.width=285;chart.height=170
        chart.data=[[row[key] for row in values] for key in keys]
        chart.categoryAxis.categoryNames=[str(row['clients']) for row in values]
        chart.categoryAxis.labels.fontName='Helvetica';chart.categoryAxis.labels.fontSize=10
        chart.valueAxis.valueMin=0
        maximum=max(max(series) for series in chart.data)
        step=100 if maximum>100 else 10
        chart.valueAxis.valueMax=math.ceil(maximum/step)*step
        chart.valueAxis.valueStep=step
        chart.valueAxis.labels.fontSize=9
        chart.valueAxis.visibleGrid=True;chart.valueAxis.gridStrokeColor=colors.HexColor('#E5E8E6')
        chart.valueAxis.gridStrokeWidth=.4
        chart.bars[0].fillColor=palette[0];chart.bars[1].fillColor=palette[1]
        chart.bars.strokeColor=None;chart.barSpacing=3;chart.groupSpacing=16
        drawing.add(chart)
        drawing.add(String(x,274,title,fontName='Helvetica-Bold',fontSize=12))
        drawing.add(String(x+65,55,'Concurrent customers',fontName='Helvetica',fontSize=10))
        drawing.add(String(x,38,unit,fontName='Helvetica',fontSize=9,fillColor=colors.HexColor('#555555')))
    for x,label,color in [(285,'Baseline',palette[0]),(395,'Connection pool',palette[1])]:
        drawing.add(Rect(x,13,10,10,fillColor=color,strokeColor=None))
        drawing.add(String(x+16,14,label,fontName='Helvetica',fontSize=10))
    renderSVG.drawToFile(drawing,str(directory/'comparison.svg'))
    print(directory/'comparison.svg')
    return drawing

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--campaign',required=True)
    main(parser.parse_args().campaign)
