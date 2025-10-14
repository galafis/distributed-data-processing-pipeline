"""
S3 utilities for data pipeline.

Provides helper functions for working with S3.

Author: Gabriel Demetrios Lafis
"""

import os
import logging
from typing import List, Optional
from pathlib import Path

logger = logging.getLogger(__name__)


class S3Utils:
    """Utilities for working with AWS S3."""
    
    def __init__(self, region: str = 'us-east-1'):
        """
        Initialize S3 utilities.
        
        Parameters
        ----------
        region : str
            AWS region
        """
        self.region = region
        self._s3_client = None
    
    @property
    def s3_client(self):
        """Get or create S3 client."""
        if self._s3_client is None:
            try:
                import boto3
                self._s3_client = boto3.client('s3', region_name=self.region)
            except ImportError:
                logger.error("boto3 not installed. Install with: pip install boto3")
                raise
        return self._s3_client
    
    def list_files(
        self,
        bucket: str,
        prefix: str = '',
        suffix: str = ''
    ) -> List[str]:
        """
        List files in S3 bucket.
        
        Parameters
        ----------
        bucket : str
            S3 bucket name
        prefix : str, optional
            Prefix filter
        suffix : str, optional
            Suffix filter (e.g., '.parquet')
            
        Returns
        -------
        list
            List of S3 keys
        """
        logger.info(f"Listing files in s3://{bucket}/{prefix}")
        
        paginator = self.s3_client.get_paginator('list_objects_v2')
        pages = paginator.paginate(Bucket=bucket, Prefix=prefix)
        
        files = []
        for page in pages:
            if 'Contents' in page:
                for obj in page['Contents']:
                    key = obj['Key']
                    if suffix and not key.endswith(suffix):
                        continue
                    files.append(key)
        
        logger.info(f"Found {len(files)} files")
        return files
    
    def upload_file(
        self,
        local_path: str,
        bucket: str,
        s3_key: str
    ) -> bool:
        """
        Upload file to S3.
        
        Parameters
        ----------
        local_path : str
            Local file path
        bucket : str
            S3 bucket name
        s3_key : str
            S3 key (path in bucket)
            
        Returns
        -------
        bool
            True if successful
        """
        logger.info(f"Uploading {local_path} to s3://{bucket}/{s3_key}")
        
        try:
            self.s3_client.upload_file(local_path, bucket, s3_key)
            logger.info("Upload successful")
            return True
        except Exception as e:
            logger.error(f"Upload failed: {e}")
            return False
    
    def download_file(
        self,
        bucket: str,
        s3_key: str,
        local_path: str
    ) -> bool:
        """
        Download file from S3.
        
        Parameters
        ----------
        bucket : str
            S3 bucket name
        s3_key : str
            S3 key (path in bucket)
        local_path : str
            Local file path to save to
            
        Returns
        -------
        bool
            True if successful
        """
        logger.info(f"Downloading s3://{bucket}/{s3_key} to {local_path}")
        
        try:
            # Create parent directories if they don't exist
            Path(local_path).parent.mkdir(parents=True, exist_ok=True)
            
            self.s3_client.download_file(bucket, s3_key, local_path)
            logger.info("Download successful")
            return True
        except Exception as e:
            logger.error(f"Download failed: {e}")
            return False
    
    def delete_file(self, bucket: str, s3_key: str) -> bool:
        """
        Delete file from S3.
        
        Parameters
        ----------
        bucket : str
            S3 bucket name
        s3_key : str
            S3 key (path in bucket)
            
        Returns
        -------
        bool
            True if successful
        """
        logger.info(f"Deleting s3://{bucket}/{s3_key}")
        
        try:
            self.s3_client.delete_object(Bucket=bucket, Key=s3_key)
            logger.info("Delete successful")
            return True
        except Exception as e:
            logger.error(f"Delete failed: {e}")
            return False
    
    def file_exists(self, bucket: str, s3_key: str) -> bool:
        """
        Check if file exists in S3.
        
        Parameters
        ----------
        bucket : str
            S3 bucket name
        s3_key : str
            S3 key (path in bucket)
            
        Returns
        -------
        bool
            True if file exists
        """
        try:
            self.s3_client.head_object(Bucket=bucket, Key=s3_key)
            return True
        except:
            return False
    
    def get_file_size(self, bucket: str, s3_key: str) -> Optional[int]:
        """
        Get file size in bytes.
        
        Parameters
        ----------
        bucket : str
            S3 bucket name
        s3_key : str
            S3 key (path in bucket)
            
        Returns
        -------
        int or None
            File size in bytes, None if file doesn't exist
        """
        try:
            response = self.s3_client.head_object(Bucket=bucket, Key=s3_key)
            return response['ContentLength']
        except:
            return None
    
    @staticmethod
    def parse_s3_path(s3_path: str) -> tuple:
        """
        Parse S3 path into bucket and key.
        
        Parameters
        ----------
        s3_path : str
            S3 path (e.g., 's3://bucket/key/path')
            
        Returns
        -------
        tuple
            (bucket, key)
        """
        if not s3_path.startswith('s3://'):
            raise ValueError(f"Invalid S3 path: {s3_path}")
        
        path_parts = s3_path[5:].split('/', 1)
        bucket = path_parts[0]
        key = path_parts[1] if len(path_parts) > 1 else ''
        
        return bucket, key
