"""Run the existing unittest suite and retain machine-readable execution evidence."""

from __future__ import annotations

import time
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


class XmlResult(unittest.TextTestResult):
    def startTest(self, test):
        super().startTest(test)
        self.started_at = time.monotonic()
        self.current = ET.SubElement(self.xml, "testcase", {
            "classname": test.__class__.__module__ + "." + test.__class__.__name__,
            "name": test._testMethodName,
        })

    def stopTest(self, test):
        self.current.set("time", str(time.monotonic() - self.started_at))
        super().stopTest(test)

    def addFailure(self, test, err):
        super().addFailure(test, err)
        ET.SubElement(self.current, "failure").text = self._exc_info_to_string(err, test)

    def addError(self, test, err):
        super().addError(test, err)
        ET.SubElement(self.current, "error").text = self._exc_info_to_string(err, test)

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        ET.SubElement(self.current, "skipped", {"message": reason})

    def addSubTest(self, test, subtest, err):
        super().addSubTest(test, subtest, err)
        if err is not None:
            kind = "failure" if issubclass(err[0], test.failureException) else "error"
            ET.SubElement(self.current, kind).text = self._exc_info_to_string(err, test)


if __name__ == "__main__":
    import os
    import sys

    repo = Path(__file__).resolve().parents[1]
    os.chdir(repo)
    sys.path.insert(0, str(repo))
    XmlResult.xml = ET.Element("testsuite", {"name": "Firebase Functions"})
    suite = unittest.defaultTestLoader.discover("functions/tests", pattern="test_*.py")
    result = unittest.TextTestRunner(verbosity=2, resultclass=XmlResult).run(suite)
    for key, value in {
        "tests": result.testsRun, "failures": len(result.failures),
        "errors": len(result.errors), "skipped": len(result.skipped),
    }.items():
        result.xml.set(key, str(value))
    output = repo / "build/rooms-evidence/local/functions.xml"
    output.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(result.xml).write(output, encoding="utf-8", xml_declaration=True)
    print(f"Informe: {output}")
    raise SystemExit(0 if result.wasSuccessful() and result.testsRun > len(result.skipped) else 1)
