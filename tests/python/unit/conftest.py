"""
Configuration and fixtures for unit tests.

Author: Gabriel Demetrios Lafis
"""

import sys
from unittest.mock import MagicMock


# Mock pyspark for unit tests - only if not already imported
if 'pyspark' not in sys.modules:
    sys.modules['pyspark'] = MagicMock()
    sys.modules['pyspark.sql'] = MagicMock()
