import os
import sys
import time
import argparse
import unittest
from pathlib import Path

# Ensure project root is on sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

def build_suite(tier="all", feature=None):
    """Discovers and constructs the test suite based on tier and optional feature filter."""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    tests_dir = PROJECT_ROOT / "tests"

    tiers_to_run = []
    if tier in ("1", "all"):
        tiers_to_run.append(("Tier 1: Feature Coverage", tests_dir / "tier1_features"))
    if tier in ("2", "all"):
        tiers_to_run.append(("Tier 2: Boundary & Corner Cases", tests_dir / "tier2_boundaries"))
    if tier in ("3", "all"):
        tiers_to_run.append(("Tier 3: Cross-Feature Combinations", tests_dir / "tier3_combinations"))
    if tier in ("4", "all"):
        tiers_to_run.append(("Tier 4: Real-World Scenarios", tests_dir / "tier4_scenarios"))

    for tier_name, tier_path in tiers_to_run:
        if not tier_path.exists():
            continue
        pattern = f"*f{feature.lower().replace('f', '')}*.py" if feature else "*.py"
        tier_suite = loader.discover(
            start_dir=str(tier_path),
            pattern=pattern,
            top_level_dir=str(PROJECT_ROOT)
        )
        suite.addTest(tier_suite)

    return suite

def main():
    parser = argparse.ArgumentParser(
        description="OwnVoice E2E Test Suite Runner (Snapdragon ARM64 & x64 Windows)"
    )
    parser.add_argument(
        "--tier",
        choices=["1", "2", "3", "4", "all"],
        default="all",
        help="Specify which tier of tests to execute (default: all)"
    )
    parser.add_argument(
        "--feature",
        type=str,
        default=None,
        help="Filter tests by feature identifier (e.g., F01, F13)"
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose test output"
    )

    args = parser.parse_args()

    print("=" * 80)
    print(" OwnVoice Comprehensive E2E Test Suite Runner")
    print(f" Target Platform : Windows (ARM64 / x64)")
    print(f" Executing Tier  : {args.tier.upper()}")
    if args.feature:
        print(f" Feature Filter  : {args.feature.upper()}")
    print("=" * 80)

    start_time = time.perf_counter()
    suite = build_suite(tier=args.tier, feature=args.feature)
    
    test_count = suite.countTestCases()
    if test_count == 0:
        print(f"\n[WARNING] No tests discovered for Tier: {args.tier}, Feature: {args.feature}")
        return 2

    print(f"Discovered {test_count} tests across requested suites.\n")

    verbosity = 2 if args.verbose else 1
    runner = unittest.TextTestRunner(verbosity=verbosity)
    result = runner.run(suite)
    elapsed = time.perf_counter() - start_time

    print("\n" + "=" * 80)
    print(" TEST EXECUTION SUMMARY")
    print("=" * 80)
    print(f" Total Tests Executed : {result.testsRun}")
    print(f" Tests Passed         : {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f" Failures             : {len(result.failures)}")
    print(f" Errors               : {len(result.errors)}")
    print(f" Skipped              : {len(result.skipped)}")
    print(f" Elapsed Time         : {elapsed:.2f} seconds")
    print("=" * 80)

    if result.wasSuccessful():
        print("\n[SUCCESS] ALL EXECUTED TESTS PASSED CLEANLY (Exit Code 0)\n")
        return 0
    else:
        print(f"\n[FAILURE] TEST SUITE FAILED WITH {len(result.failures)} FAILURES AND {len(result.errors)} ERRORS (Exit Code 1)\n")
        return 1

if __name__ == "__main__":
    sys.exit(main())
