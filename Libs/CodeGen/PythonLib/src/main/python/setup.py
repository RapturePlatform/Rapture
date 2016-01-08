#!/usr/bin/env python
"""raptureAPI: Incapture Technologies, LLC
"""

from raptureAPI import __version__
from setuptools import setup
from os import path 

DISTNAME = 'raptureAPI'
PACKAGES = ['raptureAPI']

AUTHOR = "Incapture Technologies LLC"
AUTHOR_EMAIL = 'info@incapturetechnologies.com'
LICENSE = 'Incapture Technologies LLC'
DESCRIPTION = 'raptureAPI: Incapture Technologies LLC'

LONG_DESCRIPTION    = """
=============================================
raptureAPI: Incapture Technologies LLC
=============================================
"""

PLATFORMS = 'any'
URL  = "http://incapturetechnologies.com/"
DOWNLOAD_URL  = ""

# here = path.abspath(path.dirname(__file__))
# with open(path.join(here, 'VERSION')) as f:
#     __version__ = f.read()

setup(name=DISTNAME,
      version=__version__,
      maintainer=AUTHOR,
      maintainer_email=AUTHOR_EMAIL,
      description=DESCRIPTION,
      long_description=LONG_DESCRIPTION,
      url=URL,
      download_url=DOWNLOAD_URL,
      license=LICENSE,
      packages=PACKAGES,
     )

