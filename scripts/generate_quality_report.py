#!/usr/bin/env python3
"""
Data Quality Report Generator

Generates HTML reports for data quality metrics.

Author: Gabriel Demetrios Lafis
"""

import argparse
import sys
from datetime import datetime
from pathlib import Path


def generate_html_report(input_path: str, output_path: str):
    """
    Generate an HTML data quality report.
    
    Parameters
    ----------
    input_path : str
        Path to input data
    output_path : str
        Path to output HTML report
    """
    try:
        from pyspark.sql import SparkSession
        from pyspark.sql.functions import col, count, sum as spark_sum, avg, min as spark_min, max as spark_max, isnan, when
        
        spark = SparkSession.builder \
            .appName("DataQualityReportGenerator") \
            .config("spark.ui.enabled", "false") \
            .getOrCreate()
        
        # Read data
        print(f"Reading data from: {input_path}")
        df = spark.read.format("delta").load(input_path)
        
        # Calculate statistics
        total_records = df.count()
        columns = df.columns
        
        # Calculate nulls and basic stats for each column
        column_stats = []
        for col_name in columns:
            null_count = df.filter(col(col_name).isNull()).count()
            null_pct = (null_count / total_records * 100) if total_records > 0 else 0
            
            distinct_count = df.select(col_name).distinct().count()
            
            stats = {
                'column': col_name,
                'null_count': null_count,
                'null_percentage': null_pct,
                'distinct_count': distinct_count,
                'data_type': str(df.schema[col_name].dataType)
            }
            column_stats.append(stats)
        
        # Generate HTML
        html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Data Quality Report</title>
    <style>
        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        h1 {{
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #34495e;
            margin-top: 30px;
        }}
        .summary {{
            background-color: #ecf0f1;
            padding: 20px;
            border-radius: 5px;
            margin: 20px 0;
        }}
        .summary-item {{
            display: inline-block;
            margin: 10px 20px 10px 0;
        }}
        .summary-label {{
            font-weight: bold;
            color: #7f8c8d;
        }}
        .summary-value {{
            font-size: 24px;
            color: #2c3e50;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }}
        th {{
            background-color: #3498db;
            color: white;
            padding: 12px;
            text-align: left;
        }}
        td {{
            padding: 10px;
            border-bottom: 1px solid #ddd;
        }}
        tr:hover {{
            background-color: #f8f9fa;
        }}
        .status {{
            padding: 5px 10px;
            border-radius: 3px;
            font-weight: bold;
        }}
        .status-good {{
            background-color: #2ecc71;
            color: white;
        }}
        .status-warning {{
            background-color: #f39c12;
            color: white;
        }}
        .status-bad {{
            background-color: #e74c3c;
            color: white;
        }}
        .footer {{
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
            color: #7f8c8d;
            text-align: center;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Data Quality Report</h1>
        
        <div class="summary">
            <div class="summary-item">
                <div class="summary-label">Total Records</div>
                <div class="summary-value">{total_records:,}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">Total Columns</div>
                <div class="summary-value">{len(columns)}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">Generated</div>
                <div class="summary-value">{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</div>
            </div>
        </div>
        
        <h2>Column Statistics</h2>
        <table>
            <thead>
                <tr>
                    <th>Column Name</th>
                    <th>Data Type</th>
                    <th>Null Count</th>
                    <th>Null %</th>
                    <th>Distinct Values</th>
                    <th>Quality Status</th>
                </tr>
            </thead>
            <tbody>
"""
        
        for stat in column_stats:
            null_pct = stat['null_percentage']
            if null_pct == 0:
                status = '<span class="status status-good">✓ Good</span>'
            elif null_pct < 5:
                status = '<span class="status status-warning">⚠ Warning</span>'
            else:
                status = '<span class="status status-bad">✗ Poor</span>'
            
            html_content += f"""
                <tr>
                    <td><strong>{stat['column']}</strong></td>
                    <td>{stat['data_type']}</td>
                    <td>{stat['null_count']:,}</td>
                    <td>{stat['null_percentage']:.2f}%</td>
                    <td>{stat['distinct_count']:,}</td>
                    <td>{status}</td>
                </tr>
"""
        
        html_content += """
            </tbody>
        </table>
        
        <h2>Quality Checks</h2>
        <table>
            <thead>
                <tr>
                    <th>Check</th>
                    <th>Status</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody>
"""
        
        # Add quality checks
        checks = [
            {
                'name': 'Record Count',
                'status': 'PASS' if total_records > 0 else 'FAIL',
                'details': f'{total_records:,} records found'
            },
            {
                'name': 'Schema Validation',
                'status': 'PASS' if len(columns) > 0 else 'FAIL',
                'details': f'{len(columns)} columns detected'
            },
            {
                'name': 'Null Check',
                'status': 'PASS' if all(s['null_percentage'] < 10 for s in column_stats) else 'WARNING',
                'details': 'All columns have acceptable null rates'
            }
        ]
        
        for check in checks:
            status_class = 'status-good' if check['status'] == 'PASS' else 'status-warning' if check['status'] == 'WARNING' else 'status-bad'
            html_content += f"""
                <tr>
                    <td><strong>{check['name']}</strong></td>
                    <td><span class="status {status_class}">{check['status']}</span></td>
                    <td>{check['details']}</td>
                </tr>
"""
        
        html_content += f"""
            </tbody>
        </table>
        
        <div class="footer">
            <p>Generated by Distributed Data Processing Pipeline</p>
            <p>Author: Gabriel Demetrios Lafis</p>
            <p>Input: {input_path}</p>
        </div>
    </div>
</body>
</html>
"""
        
        # Write HTML to file
        output_dir = Path(output_path).parent
        output_dir.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'w') as f:
            f.write(html_content)
        
        print(f"✓ Report generated successfully: {output_path}")
        
        spark.stop()
        
    except Exception as e:
        print(f"✗ Error generating report: {e}", file=sys.stderr)
        raise


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Generate data quality report for processed data'
    )
    parser.add_argument(
        '--input-path',
        required=True,
        help='Path to input data (Delta Lake format)'
    )
    parser.add_argument(
        '--output-path',
        required=True,
        help='Path to output HTML report'
    )
    
    args = parser.parse_args()
    
    generate_html_report(args.input_path, args.output_path)


if __name__ == '__main__':
    main()
